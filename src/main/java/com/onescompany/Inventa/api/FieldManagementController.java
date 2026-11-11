package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.*;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;
import com.onescompany.Inventa.security.AppUser;
import com.onescompany.Inventa.security.AppUserRepository;
import com.onescompany.Inventa.security.UserRole;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/fields")
@Transactional
public class FieldManagementController {
    private final PlantationFieldRepository fields;
    private final AppUserRepository users;

    public FieldManagementController(PlantationFieldRepository fields, AppUserRepository users) {
        this.fields = fields;
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<FieldResponse> listFields(Principal principal) {
        AppUser account = currentAccount(principal);
        List<PlantationField> visible = account.getRole() == UserRole.FIELD_MANAGER
                ? List.of(requireManagerField(account))
                : fields.findAll();
        return visible.stream()
                .filter(field -> account.getRole() == UserRole.DIRECTOR || field.isActive())
                .map(this::response)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public FieldResponse createField(@Valid @RequestBody FieldCreateRequest request) {
        String name = request.name().trim();
        if (fields.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A field with that name already exists.");
        }
        PlantationField field = fields.save(new PlantationField(
                name,
                request.location().trim(),
                request.description() == null || request.description().isBlank()
                        ? null
                        : request.description().trim()));
        return response(field);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public FieldResponse updateField(
            @PathVariable Long id,
            @Valid @RequestBody FieldCreateRequest request) {
        PlantationField field = fields.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
        String name = request.name().trim();
        if (!field.getName().equalsIgnoreCase(name) && fields.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A field with that name already exists.");
        }
        field.updateDetails(name, request.location().trim(),
                request.description() == null || request.description().isBlank()
                        ? null
                        : request.description().trim());
        return response(field);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public FieldResponse archiveField(@PathVariable Long id) {
        PlantationField field = fields.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
        if (field.isActive()) {
            field.archive();
            users.findAll().stream()
                    .filter(user -> user.getField() != null && user.getField().getId().equals(id))
                    .forEach(user -> {
                        user.updateEnabled(false);
                        if (user.getPayrollWorker() != null) {
                            user.getPayrollWorker().updateActive(false);
                        }
                    });
        }
        return response(field);
    }

    private AppUser currentAccount(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
    }

    private PlantationField requireManagerField(AppUser manager) {
        if (manager.getField() == null || !manager.getField().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This field-manager account has no assigned field.");
        }
        return manager.getField();
    }

    private FieldResponse response(PlantationField field) {
        List<AccountResponse> managers = users.findAll().stream()
                .filter(user -> user.getRole() == UserRole.FIELD_MANAGER)
                .filter(user -> !user.isDeleted())
                .filter(user -> user.getField() != null && user.getField().getId().equals(field.getId()))
                .map(user -> new AccountResponse(
                        user.getId(), user.getUsername(), user.getRole(), user.isEnabled(),
                        user.getPhoneNumber() == null ? "" : user.getPhoneNumber(),
                        field.getId(), field.getName(),
                        user.getPayrollWorker() == null ? null : user.getPayrollWorker().getId(),
                        user.getPayrollWorker() == null ? null : user.getPayrollWorker().getSalary(),
                        user.getPayrollWorker() == null
                                ? null
                                : user.getPayrollWorker().getMonthlyRequiredHours()))
                .toList();
        return new FieldResponse(field.getId(), field.getName(), field.getLocation(),
                field.getDescription(), field.isActive(), managers);
    }
}

package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.*;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.security.AppUser;
import com.onescompany.Inventa.security.AppUserRepository;
import com.onescompany.Inventa.security.UserRole;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@Transactional
@PreAuthorize("hasRole('DIRECTOR')")
public class AccountManagementController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PlantationFieldRepository fields;

    public AccountManagementController(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            PlantationFieldRepository fields) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.fields = fields;
    }

    @GetMapping
    public List<AccountResponse> accounts() {
        return users.findAll().stream()
                .map(this::accountResponse)
                .toList();
    }

    @PostMapping
    public AccountResponse createAccount(@Valid @RequestBody AccountCreateRequest request) {
        String username = request.username().trim();
        if (request.role() == UserRole.DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only assistant and field accounts can be created here.");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that username already exists.");
        }
        PlantationField field = null;
        if (request.role() == UserRole.FIELD_MANAGER) {
            if (request.fieldId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A field manager must be assigned to one field.");
            }
            field = requireField(request.fieldId());
        } else if (request.fieldId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assistant accounts are central and cannot be assigned to a field.");
        }
        AppUser account = users.save(new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                request.role(),
                request.phoneNumber().trim(),
                field));
        return accountResponse(account);
    }

    @PutMapping("/{id}/enabled")
    public AccountResponse updateAccountEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AccountEnabledRequest request) {
        AppUser account = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
        if (account.getRole() == UserRole.DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Director accounts cannot be disabled here.");
        }
        account.updateEnabled(request.enabled());
        return accountResponse(account);
    }

    @PutMapping("/{id}/field")
    public AccountResponse assignField(
            @PathVariable Long id,
            @Valid @RequestBody AccountFieldRequest request) {
        AppUser account = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
        if (account.getRole() != UserRole.FIELD_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only field-manager accounts can be assigned to a field.");
        }
        account.assignField(requireField(request.fieldId()));
        return accountResponse(account);
    }

    private PlantationField requireField(Long id) {
        return fields.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
    }

    private AccountResponse accountResponse(AppUser account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.isEnabled(),
                account.getPhoneNumber() == null ? "" : account.getPhoneNumber(),
                account.getField() == null ? null : account.getField().getId(),
                account.getField() == null ? null : account.getField().getName());
    }
}

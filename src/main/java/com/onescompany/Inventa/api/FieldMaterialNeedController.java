package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.FieldMaterialNeedRequest;
import static com.onescompany.Inventa.api.ApiDtos.FieldMaterialNeedPurchaseRequest;
import static com.onescompany.Inventa.api.ApiDtos.FieldMaterialNeedResponse;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.domain.FieldMaterialNeed;
import com.onescompany.Inventa.domain.FieldMaterialNeedRepository;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;
import com.onescompany.Inventa.security.AppUser;
import com.onescompany.Inventa.security.AppUserRepository;
import com.onescompany.Inventa.security.PushNotificationService;
import com.onescompany.Inventa.security.UserRole;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stock/material-needs")
@Transactional
public class FieldMaterialNeedController {
    private final FieldMaterialNeedRepository needs;
    private final PlantationFieldRepository fields;
    private final AppUserRepository users;
    private final PushNotificationService pushNotifications;

    public FieldMaterialNeedController(
            FieldMaterialNeedRepository needs,
            PlantationFieldRepository fields,
            AppUserRepository users,
            PushNotificationService pushNotifications) {
        this.needs = needs;
        this.fields = fields;
        this.users = users;
        this.pushNotifications = pushNotifications;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER','TECHNICAL_MANAGER')")
    public List<FieldMaterialNeedResponse> list(
            @RequestParam(required = false) Long fieldId,
            Principal principal) {
        AppUser account = currentAccount(principal);
        if (account.getRole() == UserRole.FIELD_MANAGER) {
            Long assignedFieldId = account.getField() == null ? null : account.getField().getId();
            if (assignedFieldId == null || (fieldId != null && !fieldId.equals(assignedFieldId))) {
                return List.of();
            }
            fieldId = assignedFieldId;
        }
        Long selectedFieldId = fieldId;
        return needs.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(need -> need.getField().isActive())
                .filter(need -> selectedFieldId == null
                        || need.getField().getId().equals(selectedFieldId))
                .map(this::response)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('TECHNICAL_MANAGER')")
    public FieldMaterialNeedResponse create(
            @Valid @RequestBody FieldMaterialNeedRequest request,
            Principal principal) {
        PlantationField field = fields.findById(request.fieldId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
        if (!field.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
        }
        FieldMaterialNeed need = needs.save(new FieldMaterialNeed(
                field,
                request.materialName().trim(),
                request.unit().trim(),
                request.quantity(),
                request.notes() == null ? "" : request.notes().trim(),
                principal.getName()));
        pushNotifications.notifyRole(
                UserRole.ASSISTANT,
                "Besoins matériels pour " + field.getName(),
                request.quantity() + " " + request.unit().trim() + " de "
                        + request.materialName().trim() + " indiqués par le responsable technique.",
                "field_material_need");
        return response(need);
    }

    @PutMapping("/{id}/purchased")
    @PreAuthorize("hasRole('ASSISTANT')")
    public FieldMaterialNeedResponse updatePurchased(
            @PathVariable Long id,
            @Valid @RequestBody FieldMaterialNeedPurchaseRequest request) {
        FieldMaterialNeed need = needs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material need not found."));
        need.setPurchased(request.purchased());
        return response(need);
    }

    private AppUser currentAccount(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
    }

    private FieldMaterialNeedResponse response(FieldMaterialNeed need) {
        return new FieldMaterialNeedResponse(
                need.getId(),
                need.getField().getId(),
                need.getField().getName(),
                need.getMaterialName(),
                need.getUnit(),
                need.getQuantity(),
                need.getNotes(),
                need.getCreatedBy(),
                need.getCreatedAt(),
                need.isPurchased());
    }
}

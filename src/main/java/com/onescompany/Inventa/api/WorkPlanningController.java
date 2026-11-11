package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.domain.PlannedWork;
import com.onescompany.Inventa.domain.PlannedWorkRepository;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;
import com.onescompany.Inventa.security.AppUser;
import com.onescompany.Inventa.security.AppUserRepository;
import com.onescompany.Inventa.security.PushNotificationService;
import com.onescompany.Inventa.security.UserRole;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/work-plans")
@Transactional
public class WorkPlanningController {
    private final PlannedWorkRepository plans;
    private final PlantationFieldRepository fields;
    private final AppUserRepository users;
    private final PushNotificationService pushNotifications;

    public WorkPlanningController(
            PlannedWorkRepository plans,
            PlantationFieldRepository fields,
            AppUserRepository users,
            PushNotificationService pushNotifications) {
        this.plans = plans;
        this.fields = fields;
        this.users = users;
        this.pushNotifications = pushNotifications;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER','TECHNICAL_MANAGER')")
    public List<WorkPlanResponse> list(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Principal principal) {
        validateDates(from, to);
        AppUser account = currentUser(principal);
        List<PlannedWork> matching = account.getRole() == UserRole.FIELD_MANAGER
                ? plans.findByFieldManagerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(
                        account.getId(), to, from)
                : plans.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(
                        to, from);
        return matching.stream().map(this::response).toList();
    }

    @GetMapping("/assignees")
    @PreAuthorize("hasRole('TECHNICAL_MANAGER')")
    public List<WorkPlanAssigneeResponse> assignees() {
        return users.findAll().stream()
                .filter(user -> user.isEnabled() && user.getRole() == UserRole.FIELD_MANAGER)
                .filter(user -> user.getField() != null && user.getField().isActive())
                .map(user -> new WorkPlanAssigneeResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getField().getId(),
                        user.getField().getName()))
                .toList();
    }

    @GetMapping("/fields")
    @PreAuthorize("hasRole('TECHNICAL_MANAGER')")
    public List<WorkPlanFieldResponse> fields() {
        return fields.findAll().stream()
                .filter(PlantationField::isActive)
                .map(field -> new WorkPlanFieldResponse(
                        field.getId(),
                        field.getName(),
                        field.getLocation(),
                        field.getDescription()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('TECHNICAL_MANAGER')")
    public WorkPlanResponse create(@Valid @RequestBody WorkPlanRequest request, Principal principal) {
        validateRequest(request);
        String actor = principal.getName();
        PlantationField field = requireField(request.fieldId());
        AppUser assignee = requireAssignee(request.fieldManagerAccountId(), field);
        PlannedWork plan = plans.save(new PlannedWork(
                request.title().trim(),
                field,
                assignee,
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                actor));
        notifyAssignee(
                assignee,
                "Nouveau travail planifié",
                plan.getTitle() + " · " + field.getName() + " · "
                        + plan.getStartDate(),
                "work_plan_assigned");
        return response(plan);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TECHNICAL_MANAGER')")
    public WorkPlanResponse update(
            @PathVariable Long id,
            @Valid @RequestBody WorkPlanRequest request,
            Principal principal) {
        validateRequest(request);
        PlannedWork plan = requirePlan(id);
        AppUser previousAssignee = plan.getFieldManager();
        PlantationField field = requireField(request.fieldId());
        AppUser assignee = requireAssignee(request.fieldManagerAccountId(), field);
        try {
            plan.update(
                    request.title().trim(),
                    field,
                    assignee,
                    request.startDate(),
                    request.endDate(),
                    request.startTime(),
                    request.endTime(),
                    principal.getName());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        if (!previousAssignee.getId().equals(assignee.getId())) {
            notifyAssignee(
                    previousAssignee,
                    "Planification réattribuée",
                    "Le travail « " + plan.getTitle() + " » ne vous est plus assigné.",
                    "work_plan_reassigned");
        }
        notifyAssignee(
                assignee,
                "Planification mise à jour",
                plan.getTitle() + " · " + field.getName() + " · " + plan.getStartDate(),
                "work_plan_updated");
        return response(plan);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TECHNICAL_MANAGER')")
    public WorkPlanResponse cancel(@PathVariable Long id, Principal principal) {
        PlannedWork plan = requirePlan(id);
        try {
            plan.cancel(principal.getName());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        notifyAssignee(
                plan.getFieldManager(),
                "Planification annulée",
                "Le travail « " + plan.getTitle() + " » prévu pour "
                        + plan.getStartDate() + " a été annulé.",
                "work_plan_cancelled");
        return response(plan);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public WorkPlanResponse complete(
            @PathVariable Long id,
            Principal principal) {
        PlannedWork plan = requirePlan(id);
        AppUser account = currentUser(principal);
        if (!plan.getFieldManager().getId().equals(account.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work plan not found.");
        }
        try {
            plan.complete(principal.getName());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        pushNotifications.notifyRole(
                UserRole.TECHNICAL_MANAGER,
                "Travail planifié terminé",
                plan.getTitle() + " · " + plan.getField().getName()
                        + " · terminé par " + principal.getName(),
                "work_plan_completed");
        return response(plan);
    }

    private void validateRequest(WorkPlanRequest request) {
        validateDates(request.startDate(), request.endDate());
        if (request.startDate().equals(request.endDate()) && request.startTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A one-day plan requires a start time.");
        }
        if (request.startTime() != null && request.endTime() != null
                && request.startDate().equals(request.endDate())
                && !request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time.");
        }
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The plan date range is invalid.");
        }
    }

    private PlantationField requireField(Long id) {
        PlantationField field = fields.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
        if (!field.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
        }
        return field;
    }

    private AppUser requireAssignee(Long id, PlantationField field) {
        AppUser assignee = users.findById(id)
                .filter(AppUser::isEnabled)
                .filter(user -> user.getRole() == UserRole.FIELD_MANAGER)
                .filter(user -> user.getField() != null && user.getField().getId().equals(field.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Choose an enabled field manager assigned to this field."));
        return assignee;
    }

    private AppUser currentUser(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
    }

    private PlannedWork requirePlan(Long id) {
        return plans.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work plan not found."));
    }

    private WorkPlanResponse response(PlannedWork plan) {
        return new WorkPlanResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getField().getId(),
                plan.getField().getName(),
                plan.getFieldManager().getId(),
                plan.getFieldManager().getUsername(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStartTime(),
                plan.getEndTime(),
                plan.getCreatedBy(),
                plan.getCreatedAt(),
                plan.getUpdatedBy(),
                plan.getUpdatedAt(),
                plan.getCancelledBy(),
                plan.getCancelledAt(),
                plan.getCompletedBy(),
                plan.getCompletedAt());
    }

    private void notifyAssignee(AppUser assignee, String title, String body, String type) {
        pushNotifications.notifyUsername(assignee.getUsername(), title, body, type);
    }
}

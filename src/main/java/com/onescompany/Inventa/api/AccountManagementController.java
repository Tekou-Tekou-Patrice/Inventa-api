package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.*;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.onescompany.Inventa.domain.Worker;
import com.onescompany.Inventa.domain.WorkerRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@Transactional
@PreAuthorize("hasRole('DIRECTOR')")
public class AccountManagementController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PlantationFieldRepository fields;
    private final WorkerRepository workers;

    public AccountManagementController(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            PlantationFieldRepository fields,
            WorkerRepository workers) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.fields = fields;
        this.workers = workers;
    }

    @GetMapping
    public List<AccountResponse> accounts() {
        return users.findAll().stream()
                .filter(account -> account.getRole() != UserRole.DIRECTOR && !account.isDeleted())
                .map(this::accountResponse)
                .toList();
    }

    @PostMapping
    public AccountResponse createAccount(@Valid @RequestBody AccountCreateRequest request) {
        String username = request.username().trim();
        if (request.role() == UserRole.DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Director accounts cannot be created here.");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that username already exists.");
        }
        PlantationField field = null;
        if (request.role() == UserRole.FIELD_MANAGER) {
            if (request.fieldId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A field manager must be assigned to one field.");
            }
            if (request.monthlySalary() == null || request.monthlyRequiredHours() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "A field manager requires a monthly salary and required hours.");
            }
            field = requireField(request.fieldId());
            if (!field.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived fields cannot receive accounts.");
            }
        } else if (request.fieldId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Assistant and technical-manager accounts cannot be assigned to a field.");
        }
        Worker payrollWorker = null;
        if (request.role() == UserRole.FIELD_MANAGER) {
            payrollWorker = workers.save(new Worker(
                    username,
                    "Responsable de terrain",
                    request.monthlySalary(),
                    request.monthlyRequiredHours(),
                    request.phoneNumber().trim(),
                    field));
            payrollWorker.updateSalary(request.monthlySalary());
        }
        AppUser account = users.save(new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                request.role(),
                request.phoneNumber().trim(),
                field,
                payrollWorker));
        return accountResponse(account);
    }

    @PutMapping("/{id}/enabled")
    public AccountResponse updateAccountEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AccountEnabledRequest request) {
        AppUser account = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
        if (account.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found.");
        }
        if (account.getRole() == UserRole.DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Director accounts cannot be disabled here.");
        }
        if (request.enabled() && account.getField() != null && !account.getField().isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Reassign this account to an active field before reactivating it.");
        }
        account.updateEnabled(request.enabled());
        if (account.getPayrollWorker() != null) {
            account.getPayrollWorker().updateActive(request.enabled());
        }
        return accountResponse(account);
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request) {
        AppUser account = requireManagedAccount(id);
        String username = request.username().trim();
        boolean usernameInUse = users.findAll().stream()
                .anyMatch(user -> !user.getId().equals(id)
                        && user.getUsername().equalsIgnoreCase(username));
        if (usernameInUse) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that username already exists.");
        }

        PlantationField field = null;
        if (account.getRole() == UserRole.FIELD_MANAGER) {
            if (request.fieldId() == null || request.monthlySalary() == null
                    || request.monthlyRequiredHours() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Field managers require a field, monthly salary and required hours.");
            }
            field = requireField(request.fieldId());
            if (!field.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived fields cannot receive accounts.");
            }
            if (account.getPayrollWorker() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The payroll profile is missing.");
            }
        } else if (request.fieldId() != null || request.monthlySalary() != null
                || request.monthlyRequiredHours() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only field managers can have a field and payroll settings.");
        }

        String password = request.password() == null || request.password().isBlank()
                ? null
                : passwordEncoder.encode(request.password());
        account.updateCredentials(username, password, request.phoneNumber().trim());
        if (account.getPayrollWorker() != null) {
            Worker payrollWorker = account.getPayrollWorker();
            payrollWorker.updateName(username);
            payrollWorker.updatePhoneNumber(request.phoneNumber().trim());
            payrollWorker.assignField(field);
            payrollWorker.updateSalary(request.monthlySalary());
            payrollWorker.updateMonthlyRequiredHours(request.monthlyRequiredHours());
        }
        account.assignField(field);
        return accountResponse(account);
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        AppUser account = requireManagedAccount(id);
        if (account.getPayrollWorker() != null) {
            account.getPayrollWorker().updateActive(false);
        }
        account.deleteFromManagement();
    }

    @PutMapping("/{id}/field")
    public AccountResponse assignField(
            @PathVariable Long id,
            @Valid @RequestBody AccountFieldRequest request) {
        AppUser account = requireManagedAccount(id);
        if (account.getRole() != UserRole.FIELD_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only field-manager accounts can be assigned to a field.");
        }
        PlantationField field = requireField(request.fieldId());
        if (!field.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived fields cannot receive accounts.");
        }
        account.assignField(field);
        if (account.getPayrollWorker() != null) {
            account.getPayrollWorker().assignField(field);
        }
        return accountResponse(account);
    }

    private PlantationField requireField(Long id) {
        PlantationField field = fields.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
        if (!field.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
        }
        return field;
    }

    private AppUser requireManagedAccount(Long id) {
        AppUser account = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
        if (account.getRole() == UserRole.DIRECTOR || account.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Director accounts cannot be managed here.");
        }
        return account;
    }

    private AccountResponse accountResponse(AppUser account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.isEnabled(),
                account.getPhoneNumber() == null ? "" : account.getPhoneNumber(),
                account.getField() == null ? null : account.getField().getId(),
                account.getField() == null ? null : account.getField().getName(),
                account.getPayrollWorker() == null ? null : account.getPayrollWorker().getId(),
                account.getPayrollWorker() == null ? null : account.getPayrollWorker().getSalary(),
                account.getPayrollWorker() == null
                        ? null
                        : account.getPayrollWorker().getMonthlyRequiredHours());
    }
}

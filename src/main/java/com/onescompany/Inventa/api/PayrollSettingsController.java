package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.PayrollSettingsRequest;
import static com.onescompany.Inventa.api.ApiDtos.PayrollSettingsResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.onescompany.Inventa.domain.PayrollSettings;
import com.onescompany.Inventa.domain.PayrollSettingsRepository;
import com.onescompany.Inventa.domain.WorkerRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payroll/settings")
@Transactional
public class PayrollSettingsController {
    private final PayrollSettingsRepository settings;
    private final WorkerRepository workers;

    public PayrollSettingsController(
            PayrollSettingsRepository settings,
            WorkerRepository workers) {
        this.settings = settings;
        this.workers = workers;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public PayrollSettingsResponse getSettings() {
        PayrollSettings value = settings.findById(PayrollSettings.SINGLETON_ID)
                .orElseGet(() -> settings.save(new PayrollSettings(90_000, 208)));
        return response(value);
    }

    @PutMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public PayrollSettingsResponse updateSettings(@Valid @RequestBody PayrollSettingsRequest request) {
        PayrollSettings value = settings.findById(PayrollSettings.SINGLETON_ID)
                .orElseGet(() -> settings.save(new PayrollSettings(
                        request.monthlySalary(), request.monthlyRequiredHours())));
        value.update(request.monthlySalary(), request.monthlyRequiredHours());
        workers.findAll().forEach(worker -> worker.updateDefaultSalary(request.monthlySalary()));
        return response(value);
    }

    private PayrollSettingsResponse response(PayrollSettings value) {
        return new PayrollSettingsResponse(value.getMonthlySalary(), value.getMonthlyRequiredHours());
    }
}

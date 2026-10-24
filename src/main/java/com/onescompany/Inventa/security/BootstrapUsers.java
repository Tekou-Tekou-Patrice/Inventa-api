package com.onescompany.Inventa.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.onescompany.Inventa.domain.PayrollSettings;
import com.onescompany.Inventa.domain.PayrollSettingsRepository;

@Configuration
public class BootstrapUsers {
    @Bean
    @Order(0)
    CommandLineRunner createBootstrapUsers(
            AppUserRepository users,
            PayrollSettingsRepository payrollSettings,
            DirectorRegistrationGateRepository registrationGates,
            PasswordEncoder passwordEncoder,
            @Value("${inventa.bootstrap.director.username:}") String directorUsername,
            @Value("${inventa.bootstrap.director.password:}") String directorPassword,
            @Value("${inventa.bootstrap.assistant.username:}") String assistantUsername,
            @Value("${inventa.bootstrap.assistant.password:}") String assistantPassword,
            @Value("${inventa.bootstrap.assistant.phone-number:}") String assistantPhoneNumber,
            @Value("${inventa.bootstrap.field-manager.username:}") String fieldManagerUsername,
            @Value("${inventa.bootstrap.field-manager.password:}") String fieldManagerPassword,
            @Value("${inventa.bootstrap.field-manager.phone-number:}") String fieldManagerPhoneNumber) {
        return args -> {
            createUser(users, passwordEncoder, directorUsername, directorPassword, "", UserRole.DIRECTOR);
            createUser(users, passwordEncoder, assistantUsername, assistantPassword,
                    assistantPhoneNumber, UserRole.ASSISTANT);
            createUser(users, passwordEncoder, fieldManagerUsername, fieldManagerPassword,
                    fieldManagerPhoneNumber, UserRole.FIELD_MANAGER);
            payrollSettings.findById(PayrollSettings.SINGLETON_ID)
                    .orElseGet(() -> payrollSettings.save(new PayrollSettings(90_000, 208)));
            registrationGates.findById(DirectorRegistrationGate.SINGLETON_ID)
                    .orElseGet(() -> registrationGates.save(new DirectorRegistrationGate()));
        };
    }

    private void createUser(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            String username,
            String password,
            String phoneNumber,
            UserRole role) {
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || password.length() < 5 || password.length() > 12) {
            throw new IllegalStateException(
                    "Bootstrap account " + role + " requires a username and a password between 5 and 12 characters.");
        }
        if (role != UserRole.DIRECTOR && phoneNumber.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap account " + role + " requires a phone number.");
        }
        AppUser existing = users.findByUsername(username).orElse(null);
        if (existing != null) {
            if (role != UserRole.DIRECTOR &&
                    (existing.getPhoneNumber() == null || existing.getPhoneNumber().isBlank())) {
                existing.updatePhoneNumber(phoneNumber.trim());
            }
            return;
        }
        users.save(new AppUser(
                username,
                passwordEncoder.encode(password),
                role,
                phoneNumber.trim(),
                null));
    }
}

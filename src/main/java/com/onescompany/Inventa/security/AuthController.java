package com.onescompany.Inventa.security;

import static com.onescompany.Inventa.api.ApiDtos.DirectorRegistrationRequest;
import static com.onescompany.Inventa.api.ApiDtos.DirectorRegistrationStatus;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository users;
    private final DirectorRegistrationGateRepository registrationGates;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AppUserRepository users,
            DirectorRegistrationGateRepository registrationGates,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.registrationGates = registrationGates;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public CurrentUser currentUser(Principal principal) {
        AppUser user = users.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated account was not found."));
        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getField() == null ? null : user.getField().getId());
    }

    @GetMapping("/director-registration")
    @Transactional(readOnly = true)
    public DirectorRegistrationStatus directorRegistrationStatus() {
        return new DirectorRegistrationStatus(!users.existsByRole(UserRole.DIRECTOR));
    }

    @PostMapping("/register-director")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CurrentUser registerDirector(@Valid @RequestBody DirectorRegistrationRequest request) {
        registrationGates.findByIdForUpdate(DirectorRegistrationGate.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Director registration gate is unavailable."));
        if (users.existsByRole(UserRole.DIRECTOR)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Un compte directeur existe déjà. Connectez-vous.");
        }
        String username = request.username().trim();
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet identifiant est déjà utilisé.");
        }
        AppUser director = users.save(new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                UserRole.DIRECTOR,
                request.phoneNumber().trim(),
                null));
        return new CurrentUser(director.getId(), director.getUsername(), director.getRole(), null);
    }

    public record CurrentUser(Long id, String username, UserRole role, Long fieldId) {
    }
}

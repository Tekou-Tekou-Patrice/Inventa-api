package com.onescompany.Inventa.api;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.security.AppUser;
import com.onescompany.Inventa.security.AppUserRepository;
import com.onescompany.Inventa.security.PushDeviceToken;
import com.onescompany.Inventa.security.PushDeviceTokenRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/push/devices")
@Transactional
public class PushDeviceController {
    private final PushDeviceTokenRepository tokens;
    private final AppUserRepository users;

    public PushDeviceController(PushDeviceTokenRepository tokens, AppUserRepository users) {
        this.tokens = tokens;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public void register(@Valid @RequestBody DeviceRegistrationRequest request, Principal principal) {
        AppUser user = users.findByUsername(principal.getName())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        PushDeviceToken token = tokens.findByToken(request.token())
                .orElseGet(() -> new PushDeviceToken(request.token(), user.getUsername(), request.platform()));
        token.update(request.token(), user.getUsername(), request.platform());
        tokens.save(token);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public void remove(@Valid @RequestBody DeviceRemovalRequest request, Principal principal) {
        PushDeviceToken stored = tokens.findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!stored.getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        tokens.delete(stored);
    }

    public record DeviceRegistrationRequest(
            @NotBlank @Size(max = 255) String token,
            @NotBlank @Size(max = 16) String platform) {
    }

    public record DeviceRemovalRequest(@NotBlank @Size(max = 255) String token) {
    }
}

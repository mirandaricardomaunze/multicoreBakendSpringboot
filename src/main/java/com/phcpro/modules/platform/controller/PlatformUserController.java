package com.phcpro.modules.platform.controller;

import com.phcpro.modules.platform.dto.CreatePlatformUserRequest;
import com.phcpro.modules.platform.dto.GrantAccessRequest;
import com.phcpro.modules.platform.dto.PlatformUserDTO;
import com.phcpro.modules.platform.service.PlatformUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestão global de utilizadores pelo superadmin. Protegido pelo caminho /api/platform/**. */
@RestController
@RequestMapping("/api/platform/users")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    public PlatformUserController(PlatformUserService platformUserService) {
        this.platformUserService = platformUserService;
    }

    @GetMapping
    public List<PlatformUserDTO> list() {
        return platformUserService.listUsers();
    }

    @PostMapping
    public PlatformUserDTO create(@Valid @RequestBody CreatePlatformUserRequest request) {
        return platformUserService.createUser(request);
    }

    @PatchMapping("/{username}/active")
    public PlatformUserDTO setActive(@PathVariable String username, @RequestBody ActiveRequest request) {
        return platformUserService.setUserActive(username, request.active());
    }

    @PatchMapping("/{username}/password")
    public void resetPassword(@PathVariable String username, @Valid @RequestBody PasswordRequest request) {
        platformUserService.resetPassword(username, request.password());
    }

    @PostMapping("/{username}/access")
    public PlatformUserDTO grantAccess(@PathVariable String username, @Valid @RequestBody GrantAccessRequest request) {
        return platformUserService.grantAccess(username, request);
    }

    @DeleteMapping("/{username}/access/{companyId}")
    public PlatformUserDTO revokeAccess(@PathVariable String username, @PathVariable Long companyId) {
        return platformUserService.revokeAccess(username, companyId);
    }

    public record ActiveRequest(boolean active) {}

    public record PasswordRequest(@NotBlank String password) {}
}

package com.phcpro.modules.users.controller;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.users.dto.AppUserDTO;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.service.AppUserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestão de utilizadores da empresa activa (ADMIN). A autorização/escopo é feita no
 * {@code AppUserService} (requireAdmin + empresa do contexto); aqui só mapeamos para DTO.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserService appUserService;

    public UserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public List<AppUserDTO> list() {
        return appUserService.getAllUsers().stream().map(UserController::toDto).toList();
    }

    @PostMapping
    public AppUserDTO create(@RequestBody CreateUserRequest request) {
        return toDto(appUserService.createUser(request.username(), request.name(), request.password(), request.role()));
    }

    @PutMapping("/{username}/name")
    public AppUserDTO updateName(@PathVariable String username, @RequestBody NameRequest request) {
        return toDto(appUserService.updateUserName(username, request.name()));
    }

    @PatchMapping("/{username}/role")
    public AppUserDTO updateRole(@PathVariable String username, @RequestBody RoleRequest request) {
        return toDto(appUserService.updateCompanyRole(username, request.role()));
    }

    private static AppUserDTO toDto(AppUser u) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String role = companyId != null ? u.getRoleForCompany(companyId) : u.getRole();
        return new AppUserDTO(u.getId(), u.getUsername(), u.getName(), role, u.isActive());
    }

    public record CreateUserRequest(@NotBlank String username, @NotBlank String name,
                                    @NotBlank String password, @NotBlank String role) {}

    public record NameRequest(@NotBlank String name) {}

    public record RoleRequest(@NotBlank String role) {}
}

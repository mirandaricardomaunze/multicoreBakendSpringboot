package mz.multicore.erp.modules.users.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.users.dto.AppUserDTO;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserService;
import mz.multicore.erp.modules.users.dto.UserSecurityRequestsDTOs;
import jakarta.validation.Valid;
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
    private final mz.multicore.erp.architecture.security.ClientIpResolver clientIpResolver;

    public UserController(AppUserService appUserService,
                          mz.multicore.erp.architecture.security.ClientIpResolver clientIpResolver) {
        this.appUserService = appUserService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public List<AppUserDTO> list() {
        return appUserService.getAllUsers().stream().map(UserController::toDto).toList();
    }

    @PostMapping
    public AppUserDTO create(@RequestBody @Valid CreateUserRequest request) {
        return toDto(appUserService.createUser(request.username(), request.name(), request.password(), request.role()));
    }

    @PutMapping("/{username}/name")
    public AppUserDTO updateName(@PathVariable String username, @RequestBody @Valid NameRequest request) {
        return toDto(appUserService.updateUserName(username, request.name()));
    }

    @PatchMapping("/{username}/role")
    public AppUserDTO updateRole(@PathVariable String username, @RequestBody @Valid RoleRequest request) {
        return toDto(appUserService.updateCompanyRole(username, request.role()));
    }

    @PostMapping("/{username}/pin")
    public void setManagerPin(@PathVariable String username, @RequestBody @Valid UserSecurityRequestsDTOs.SetManagerPinRequest request) {
        appUserService.setManagerPin(username, request.pin());
    }

    @PostMapping("/verify-pin")
    public UserSecurityRequestsDTOs.VerifyManagerPinResponse verifyManagerPin(
            @RequestBody @Valid UserSecurityRequestsDTOs.VerifyManagerPinRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        return appUserService.verifyManagerPin(request.pin(), clientIp);
    }

    @PostMapping("/{username}/reset-password")
    public void resetPassword(@PathVariable String username, @RequestBody @Valid UserSecurityRequestsDTOs.ResetUserPasswordRequest request) {
        appUserService.resetPassword(username, request.newPassword());
    }

    @PatchMapping("/{username}/status")
    public AppUserDTO toggleStatus(@PathVariable String username, @RequestBody @Valid UserSecurityRequestsDTOs.ToggleUserStatusRequest request) {
        return toDto(appUserService.toggleUserStatus(username, request.active()));
    }

    private static AppUserDTO toDto(AppUser u) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String role = companyId != null ? u.getRoleForCompany(companyId) : u.getRole();
        boolean hasPin = u.getManagerPinHash() != null && !u.getManagerPinHash().isBlank();
        return new AppUserDTO(u.getId(), u.getUsername(), u.getName(), role, u.isActive(), hasPin, u.getEmail());
    }

    public record CreateUserRequest(@NotBlank String username, @NotBlank String name,
                                    @NotBlank String password, @NotBlank String role) {}

    public record NameRequest(@NotBlank String name) {}

    public record RoleRequest(@NotBlank String role) {}
}

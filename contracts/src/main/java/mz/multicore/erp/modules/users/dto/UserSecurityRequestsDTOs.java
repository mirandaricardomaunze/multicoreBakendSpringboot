package mz.multicore.erp.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserSecurityRequestsDTOs {

    public record SetManagerPinRequest(
            @NotBlank(message = "O PIN do gerente é obrigatório.")
            @Size(min = 4, max = 8, message = "O PIN do gerente deve ter entre 4 e 8 dígitos.")
            String pin
    ) {}

    public record VerifyManagerPinRequest(
            @NotBlank(message = "O PIN é obrigatório para validação.")
            String pin
    ) {}

    public record VerifyManagerPinResponse(boolean approved, String managerName, String message) {}

    public record ResetUserPasswordRequest(
            @NotBlank(message = "A nova palavra-passe é obrigatória.")
            @Size(min = 4, max = 100, message = "A palavra-passe deve ter no mínimo 4 caracteres.")
            String newPassword
    ) {}

    public record ToggleUserStatusRequest(boolean active) {}
}

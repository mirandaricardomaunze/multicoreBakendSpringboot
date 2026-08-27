package mz.multicore.erp.modules.comercial.dto;
import jakarta.validation.constraints.NotBlank;
public record ReprintAuthorizationRequest(@NotBlank String approverUsername,
 @NotBlank String approverPassword, @NotBlank String reason, String terminalName) {}

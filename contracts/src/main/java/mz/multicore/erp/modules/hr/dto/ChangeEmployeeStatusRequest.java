package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeEmployeeStatusRequest(
        @NotBlank(message = "O estado do colaborador é obrigatório.")
        @Pattern(regexp = "(?i)ACTIVE|SUSPENDED|TERMINATED",
                message = "O estado do colaborador não é válido.")
        String status
) {}

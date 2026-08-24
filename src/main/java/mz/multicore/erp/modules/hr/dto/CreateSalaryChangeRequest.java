package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSalaryChangeRequest(
        @NotNull(message = "O colaborador é obrigatório.") Long employeeId,
        @NotNull(message = "O novo salário é obrigatório.")
        @Positive(message = "O novo salário deve ser superior a zero.") BigDecimal newSalary,
        @NotNull(message = "A data de efeito é obrigatória.") LocalDate effectiveDate,
        @NotBlank(message = "O motivo da alteração é obrigatório.") String reason,
        /** Nova função, quando a alteração também a muda. Em branco mantém a anterior. */
        @Size(max = 120) String jobTitle,
        @Size(max = 120) String department,
        @Size(max = 500) String notes
) {}

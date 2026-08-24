package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateContractRequest(
        @NotNull(message = "O colaborador é obrigatório.") Long employeeId,
        @NotBlank(message = "O tipo de contrato é obrigatório.") String contractType,
        @NotNull(message = "A data de início é obrigatória.") LocalDate startDate,
        LocalDate endDate,
        LocalDate probationEndDate,
        @NotNull(message = "O salário acordado é obrigatório.")
        @Positive(message = "O salário acordado deve ser superior a zero.") BigDecimal agreedSalary,
        @Min(value = 1, message = "O horário semanal deve ser de pelo menos 1 hora.") int weeklyHours,
        @NotBlank(message = "A função é obrigatória.")
        @Size(max = 120, message = "A função não pode exceder 120 caracteres.") String jobTitle,
        @Size(max = 200, message = "O local de trabalho não pode exceder 200 caracteres.") String workLocation,
        /** Obrigatório em contrato a termo — exigência da lei laboral. Validado no serviço. */
        @Size(max = 500, message = "O motivo do termo não pode exceder 500 caracteres.") String termReason
) {}

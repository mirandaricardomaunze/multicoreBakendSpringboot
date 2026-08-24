package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveOvertimeRateConfigRequest(
        @NotBlank(message = "O nome da configuração é obrigatório.")
        @Size(max = 120) String name,
        @NotNull(message = "A data de início de vigência é obrigatória.") LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @NotNull(message = "O acréscimo de hora extra diurna é obrigatório.")
        @Positive(message = "O multiplicador deve ser superior a zero.") BigDecimal dayMultiplier,
        @NotNull(message = "O acréscimo de hora extra nocturna é obrigatório.")
        @Positive(message = "O multiplicador deve ser superior a zero.") BigDecimal nightMultiplier,
        @NotNull(message = "O acréscimo de dia de descanso é obrigatório.")
        @Positive(message = "O multiplicador deve ser superior a zero.") BigDecimal restDayMultiplier,
        /** De onde vieram os valores. Sem isto ninguém sabe contra o quê conferir. */
        @Size(max = 500) String legalBasis
) {}

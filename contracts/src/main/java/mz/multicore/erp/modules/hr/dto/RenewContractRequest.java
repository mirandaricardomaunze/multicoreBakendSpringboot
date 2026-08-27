package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Renovação. Só se diz o que muda — o resto herda-se do contrato anterior, que fica intacto: o
 * histórico do que foi acordado é imutável.
 */
public record RenewContractRequest(
        @NotNull(message = "A data de início da renovação é obrigatória.") LocalDate startDate,
        LocalDate endDate,
        /** Nulo mantém o salário do contrato anterior. */
        @Positive(message = "O salário acordado deve ser superior a zero.") BigDecimal agreedSalary,
        @Size(max = 500, message = "O motivo do termo não pode exceder 500 caracteres.") String termReason
) {}

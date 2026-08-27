package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param compensationAmount compensação por cessação, quando aplicável. <b>Introduzida por quem
 *                           cessa, não calculada pelo sistema</b>: a fórmula é legal, varia com o
 *                           motivo e a antiguidade, e não é a IA que a decide (§6). Nulo = sem
 *                           compensação, e o acerto di-lo em vez de fingir que a calculou.
 */
public record CreateTerminationRequest(
        @NotNull(message = "Indique o colaborador.") Long employeeId,
        @NotNull(message = "Indique a data de saída.") LocalDate terminationDate,
        @NotBlank(message = "Indique o motivo da cessação.") String reason,
        boolean noticeServed,
        BigDecimal compensationAmount,
        String notes
) {}

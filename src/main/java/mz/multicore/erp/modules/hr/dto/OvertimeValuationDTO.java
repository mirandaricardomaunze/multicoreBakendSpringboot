package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;

/**
 * Horas extra apuradas e o que valem. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>As horas vêm ao lado do valor de propósito: quem recebe o recibo tem direito a ver a conta,
 * não só o resultado. {@code configName} e {@code legalBasis} dizem contra o quê conferir.
 */
public record OvertimeValuationDTO(
        BigDecimal dayHours,
        BigDecimal nightHours,
        BigDecimal restDayHours,
        BigDecimal amount,
        String configName,
        String legalBasis
) {}

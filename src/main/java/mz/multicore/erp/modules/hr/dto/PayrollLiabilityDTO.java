package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma retenção da folha e o seu estado de entrega. Ver docs/RH_COMPLETO_SPEC.md §B5.
 *
 * @param dueDate  nulo quando o prazo legal ainda não foi configurado (§6)
 * @param overdue  derivado da data contra hoje, nunca gravado — lição da caducidade da cotação
 */
public record PayrollLiabilityDTO(
        Long id,
        int year,
        int month,
        String liabilityType,
        String liabilityTypeLabel,
        BigDecimal amount,
        LocalDate dueDate,
        Long daysUntilDue,
        boolean overdue,
        String status,
        String statusLabel,
        LocalDate paymentDate,
        String paymentReference,
        String deliveredBy
) {}

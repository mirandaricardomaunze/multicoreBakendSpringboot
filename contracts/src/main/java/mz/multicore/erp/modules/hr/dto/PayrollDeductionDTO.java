package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um compromisso de desconto e o seu <b>saldo em dívida</b>.
 * Ver docs/RH_COMPLETO_SPEC.md §B6.
 *
 * @param appliedAmount     quanto já foi descontado em recibos
 * @param outstandingAmount quanto falta — apurado das linhas, nunca gravado
 * @param settled           já não há nada a descontar
 */
public record PayrollDeductionDTO(
        Long id,
        Long employeeId,
        String employeeName,
        String kind,
        String kindLabel,
        String description,
        BigDecimal principalAmount,
        BigDecimal installmentAmount,
        Integer installments,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal appliedAmount,
        BigDecimal outstandingAmount,
        boolean settled,
        boolean paidOut,
        boolean active,
        String notes
) {}

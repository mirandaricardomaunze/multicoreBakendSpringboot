package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dívida de um cliente repartida por escalão de antiguidade.
 *
 * @param oldestDueDate  vencimento mais antigo ainda por liquidar (a fatura que mais arrasta)
 * @param maxDaysOverdue maior atraso do cliente, em dias
 */
public record ClientAgingDTO(
        Long clientId,
        String clientName,
        String clientTaxId,
        BigDecimal corrente,
        BigDecimal ate30,
        BigDecimal de31a60,
        BigDecimal de61a90,
        BigDecimal maisDe90,
        BigDecimal total,
        BigDecimal overdue,
        LocalDate oldestDueDate,
        int maxDaysOverdue
) {}

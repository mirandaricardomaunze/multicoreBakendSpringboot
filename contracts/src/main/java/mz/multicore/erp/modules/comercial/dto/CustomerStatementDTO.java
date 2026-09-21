package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Extrato consolidado de conta corrente de cliente com saldo progressivo e conciliação.
 */
public record CustomerStatementDTO(
        Long clientId,
        String clientName,
        String clientTaxId,
        String address,
        String email,
        String phone,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal openingBalance, // Saldo anterior transmitido
        BigDecimal totalDebits,    // Total de novos débitos no período
        BigDecimal totalCredits,   // Total de créditos/pagamentos no período
        BigDecimal closingBalance, // Saldo final acumulado (openingBalance + debits - credits)
        BigDecimal overdueAmount,  // Total de faturas vencidas em mora
        List<CustomerStatementLineDTO> lines
) {}

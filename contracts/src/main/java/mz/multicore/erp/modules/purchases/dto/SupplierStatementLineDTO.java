package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento individual no extrato de conta corrente de fornecedor (contas a pagar).
 */
public record SupplierStatementLineDTO(
        LocalDate date,
        String documentType,      // V/FT (Compra), PG (Pagamento), NC, ND
        String documentNumber,    // Ex: V/FT-2026/1, PG-2026/004
        String description,
        String reference,
        BigDecimal debit,         // Pagamentos efetuados (reduzem dívida ao fornecedor)
        BigDecimal credit,        // Compras faturadas (aumentam dívida ao fornecedor)
        BigDecimal runningBalance // Saldo acumulado em dívida a pagar
) {}

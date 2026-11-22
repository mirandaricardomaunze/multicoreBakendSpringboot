package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento individual no extrato de conta corrente de fornecedor (contas a pagar). Débito, crédito e saldo estão
 * em moeda base; {@code documentAmount} é o valor do movimento na moeda do documento ({@code currency}, ao câmbio
 * {@code exchangeRate}) — docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3.
 */
public record SupplierStatementLineDTO(
        LocalDate date,
        String documentType,      // V/FT (Compra), PG (Pagamento), NC, ND
        String documentNumber,    // Ex: V/FT-2026/1, PG-2026/004
        String description,
        String reference,
        BigDecimal debit,         // Pagamentos efetuados (reduzem dívida ao fornecedor)
        BigDecimal credit,        // Compras faturadas (aumentam dívida ao fornecedor)
        BigDecimal runningBalance, // Saldo acumulado em dívida a pagar
        Long businessUnitId,
        String businessUnitName,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal documentAmount
) {
    public SupplierStatementLineDTO(
            LocalDate date,
            String documentType,
            String documentNumber,
            String description,
            String reference,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance,
            Long businessUnitId,
            String businessUnitName
    ) {
        this(date, documentType, documentNumber, description, reference, debit, credit, runningBalance,
                businessUnitId, businessUnitName, null, null, null);
    }

    public SupplierStatementLineDTO(
            LocalDate date,
            String documentType,
            String documentNumber,
            String description,
            String reference,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance
    ) {
        this(date, documentType, documentNumber, description, reference, debit, credit, runningBalance, null, null);
    }
}

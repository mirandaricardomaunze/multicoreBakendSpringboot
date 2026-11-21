package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Extrato consolidado de conta corrente de fornecedor com saldo progressivo a pagar, em moeda base, e a dívida no
 * fim do período em cada moeda dos documentos (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3).
 */
public record SupplierStatementDTO(
        Long supplierId,
        String supplierName,
        String supplierTaxId,
        String address,
        String email,
        String phone,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal openingBalance, // Saldo anterior em dívida
        BigDecimal totalDebits,    // Total de pagamentos efetuados no período
        BigDecimal totalCredits,   // Total de compras faturadas no período
        BigDecimal closingBalance, // Saldo final em dívida a pagar (openingBalance + credits - debits)
        List<SupplierStatementLineDTO> lines,
        List<CurrencyBalanceDTO> outstandingByCurrency
) {
    public SupplierStatementDTO(
            Long supplierId,
            String supplierName,
            String supplierTaxId,
            String address,
            String email,
            String phone,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal openingBalance,
            BigDecimal totalDebits,
            BigDecimal totalCredits,
            BigDecimal closingBalance,
            List<SupplierStatementLineDTO> lines
    ) {
        this(supplierId, supplierName, supplierTaxId, address, email, phone, startDate, endDate,
                openingBalance, totalDebits, totalCredits, closingBalance, lines, List.of());
    }
}

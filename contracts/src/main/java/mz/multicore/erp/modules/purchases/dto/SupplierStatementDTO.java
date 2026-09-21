package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Extrato consolidado de conta corrente de fornecedor com saldo progressivo a pagar.
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
        List<SupplierStatementLineDTO> lines
) {}

package com.phcpro.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Conta a pagar a fornecedor: fatura de compra com saldo em dívida.
 */
public record PayableDTO(
        Long purchaseId,
        String purchaseNumber,
        Long supplierId,
        String supplierName,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal outstanding,
        LocalDateTime purchaseDate
) {}

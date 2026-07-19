package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Recibo de liquidação de fatura (vista de tabela do painel comercial). */
public record ReceiptDTO(
        Long id,
        String receiptNumber,
        String invoiceNumber,
        String clientName,
        BigDecimal amountPaid,
        String paymentMethod,
        String status,
        LocalDateTime receiptDate
) {}

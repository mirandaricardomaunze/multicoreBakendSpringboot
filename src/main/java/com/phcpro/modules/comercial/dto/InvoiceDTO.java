package com.phcpro.modules.comercial.dto;

import com.phcpro.modules.comercial.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDTO(
    Long id,
    String invoiceNumber,
    Long clientId,
    String clientName,
    String clientTaxId,
    BigDecimal totalBeforeTax,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    BigDecimal amountPaid,
    InvoiceStatus status,
    String rejectionReason,
    List<InvoiceLineDTO> lines,
    LocalDateTime createdAt
) {}

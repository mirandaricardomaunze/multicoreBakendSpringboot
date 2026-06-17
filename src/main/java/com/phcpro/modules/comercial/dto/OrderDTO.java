package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
    Long id,
    String orderNumber,
    Long clientId,
    String clientName,
    String clientTaxId,
    String walkInName,
    BigDecimal totalBeforeTax,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    String status,
    Long invoiceId,
    List<OrderLineDTO> lines,
    LocalDateTime createdAt,
    LocalDateTime printedAt,
    int printCount,
    String lastPrintedBy
) {}

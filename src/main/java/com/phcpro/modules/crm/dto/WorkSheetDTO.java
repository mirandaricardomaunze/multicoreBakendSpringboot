package com.phcpro.modules.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkSheetDTO(
    Long id,
    Long ticketId,
    String subject,
    Long clientId,
    String clientName,
    String technicianName,
    BigDecimal hoursWorked,
    String description,
    String partsUsed,
    BigDecimal partsCost,
    BigDecimal totalValue,
    Boolean isBilled,
    LocalDateTime createdAt
) {}

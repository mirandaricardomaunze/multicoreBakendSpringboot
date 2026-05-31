package com.phcpro.modules.hr.dto;

import com.phcpro.modules.hr.model.ExpenseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseClaimDTO(
    Long id,
    Long employeeId,
    String employeeName,
    BigDecimal amount,
    String category,
    String description,
    ExpenseStatus status,
    String rejectionReason,
    LocalDateTime createdAt
) {}

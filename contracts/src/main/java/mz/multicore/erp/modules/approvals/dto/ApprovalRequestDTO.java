package mz.multicore.erp.modules.approvals.dto;

import mz.multicore.erp.modules.approvals.model.ApprovalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApprovalRequestDTO(
    Long id,
    String documentType,
    Long documentId,
    BigDecimal amount,
    String submitter,
    ApprovalStatus status,
    String requiredRole,
    String description,
    String rejectionReason,
    LocalDateTime createdAt
) {}

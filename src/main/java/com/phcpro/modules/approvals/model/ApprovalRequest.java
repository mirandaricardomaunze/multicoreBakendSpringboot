package com.phcpro.modules.approvals.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "approval_requests")
@Getter
@Setter
public class ApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false)
    private String documentType; // e.g. "EXPENSE", "INVOICE"

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "submitter", nullable = false)
    private String submitter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "required_role", nullable = false)
    private String requiredRole; // "MANAGER" or "ADMIN"

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}

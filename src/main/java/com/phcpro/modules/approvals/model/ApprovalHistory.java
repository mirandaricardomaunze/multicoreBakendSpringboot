package com.phcpro.modules.approvals.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_history")
@Getter
@Setter
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @Column(name = "action", nullable = false)
    private String action; // e.g. "SUBMIT", "APPROVE", "REJECT"

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt = LocalDateTime.now();

    @Column(name = "comments", length = 500)
    private String comments;
}

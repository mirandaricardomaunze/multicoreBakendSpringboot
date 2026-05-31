package com.phcpro.modules.hr.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_claims")
@Getter
@Setter
public class ExpenseClaim extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "category", nullable = false)
    private String category; // e.g. "TRAVEL", "MEALS", "LODGING", "OTHER"

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExpenseStatus status = ExpenseStatus.PENDING_APPROVAL;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}

package com.phcpro.modules.hr.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "vacations")
@Getter
@Setter
public class Vacation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "year_reference", nullable = false)
    private int yearReference;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, CANCELLED

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "decision_by")
    private String decisionBy;

    @Column(name = "decision_at")
    private java.time.LocalDateTime decisionAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}

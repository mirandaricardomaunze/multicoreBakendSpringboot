package com.phcpro.modules.hr.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "absences")
@Getter
@Setter
public class Absence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "absence_type", nullable = false, length = 30)
    private String absenceType; // JUSTIFIED, UNJUSTIFIED, SICK, MATERNITY, OTHER

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "has_supporting_document", nullable = false)
    private boolean hasSupportingDocument = false;
}

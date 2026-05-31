package com.phcpro.modules.crm.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "crm_work_sheets")
@Getter
@Setter
public class WorkSheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket supportTicket;

    @Column(name = "technician_name", nullable = false)
    private String technicianName;

    @Column(name = "hours_worked", nullable = false)
    private BigDecimal hoursWorked;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "parts_used", length = 500)
    private String partsUsed;

    @Column(name = "parts_cost", nullable = false)
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "is_billed", nullable = false)
    private Boolean isBilled = false;
}

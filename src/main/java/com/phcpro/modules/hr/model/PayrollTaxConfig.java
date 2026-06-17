package com.phcpro.modules.hr.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payroll_tax_configs")
@Getter
@Setter
public class PayrollTaxConfig extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "employee_inss_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal employeeInssRate;

    @Column(name = "employer_inss_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal employerInssRate;

    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lowerBound ASC")
    private List<PayrollIrpsBracket> brackets = new ArrayList<>();
}

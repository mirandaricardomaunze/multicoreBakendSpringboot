package com.phcpro.modules.hr.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_irps_brackets")
@Getter
@Setter
public class PayrollIrpsBracket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private PayrollTaxConfig config;

    @Column(name = "lower_bound", nullable = false, precision = 14, scale = 2)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", precision = 14, scale = 2)
    private BigDecimal upperBound;

    @Column(name = "rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;

    @Column(name = "fixed_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal fixedDeduction = BigDecimal.ZERO;

    @Column(name = "dependent_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal dependentDeduction = BigDecimal.ZERO;
}

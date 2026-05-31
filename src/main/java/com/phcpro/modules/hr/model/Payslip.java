package com.phcpro.modules.hr.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payslips", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "ref_year", "ref_month"})
})
@Getter
@Setter
public class Payslip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payslip_number", nullable = false, unique = true)
    private String payslipNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "ref_year", nullable = false)
    private int year;

    @Column(name = "ref_month", nullable = false)
    private int month;

    @Column(name = "base_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(name = "allowances", nullable = false, precision = 14, scale = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(name = "overtime", nullable = false, precision = 14, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;

    @Column(name = "irps_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal irpsDeduction = BigDecimal.ZERO;

    @Column(name = "inss_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal inssDeduction = BigDecimal.ZERO;

    @Column(name = "other_deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "net_pay", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, PAID, CANCELLED

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "notes", length = 500)
    private String notes;
}

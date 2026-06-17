package com.phcpro.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayslipDTO(
        Long id,
        String payslipNumber,
        Long employeeId,
        String employeeName,
        String employeeDepartment,
        int year,
        int month,
        BigDecimal baseSalary,
        BigDecimal allowances,
        BigDecimal overtime,
        BigDecimal irpsDeduction,
        BigDecimal inssDeduction,
        BigDecimal employerInss,
        BigDecimal taxableIncome,
        BigDecimal otherDeductions,
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        String status,
        LocalDate paymentDate,
        String notes
) {}

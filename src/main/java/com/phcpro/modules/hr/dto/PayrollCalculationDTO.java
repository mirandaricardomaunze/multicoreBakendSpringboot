package com.phcpro.modules.hr.dto;

import java.math.BigDecimal;

public record PayrollCalculationDTO(
        BigDecimal grossPay,
        BigDecimal taxableIncome,
        BigDecimal irps,
        BigDecimal employeeInss,
        BigDecimal employerInss,
        BigDecimal netPay,
        BigDecimal irpsRate,
        BigDecimal employeeInssRate,
        BigDecimal employerInssRate,
        String configName,
        String legalBasis
) {}

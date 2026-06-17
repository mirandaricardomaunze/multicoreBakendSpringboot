package com.phcpro.modules.hr.dto;

import java.math.BigDecimal;
import java.util.List;

public record PayrollFiscalSummaryDTO(
        int year,
        int month,
        BigDecimal grossPay,
        BigDecimal taxableIncome,
        BigDecimal irpsWithheld,
        BigDecimal employeeInss,
        BigDecimal employerInss,
        BigDecimal totalInss,
        List<PayrollFiscalLineDTO> lines
) {
    public record PayrollFiscalLineDTO(
            String employeeNumber,
            String employeeName,
            String taxId,
            String inssNumber,
            BigDecimal grossPay,
            BigDecimal taxableIncome,
            BigDecimal irps,
            BigDecimal employeeInss,
            BigDecimal employerInss
    ) {}
}

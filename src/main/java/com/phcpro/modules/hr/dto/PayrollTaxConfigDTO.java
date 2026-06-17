package com.phcpro.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PayrollTaxConfigDTO(
        Long id,
        String name,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal employeeInssRate,
        BigDecimal employerInssRate,
        String legalBasis,
        boolean active,
        List<BracketDTO> brackets
) {
    public record BracketDTO(
            BigDecimal lowerBound,
            BigDecimal upperBound,
            BigDecimal rate,
            BigDecimal fixedDeduction,
            BigDecimal dependentDeduction
    ) {}
}

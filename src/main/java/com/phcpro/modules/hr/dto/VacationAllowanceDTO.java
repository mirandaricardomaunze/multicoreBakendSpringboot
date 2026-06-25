package com.phcpro.modules.hr.dto;

import java.math.BigDecimal;

/** Subsídio de férias apurado para um pedido de férias aprovado. */
public record VacationAllowanceDTO(
        Long vacationId,
        Long employeeId,
        String employeeName,
        int days,
        BigDecimal dailyRate,
        BigDecimal amount
) {}

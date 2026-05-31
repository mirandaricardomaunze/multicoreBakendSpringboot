package com.phcpro.modules.hr.dto;

import java.math.BigDecimal;

public record EmployeeDTO(
    Long id,
    String name,
    String email,
    String department,
    BigDecimal baseSalary,
    String role
) {}

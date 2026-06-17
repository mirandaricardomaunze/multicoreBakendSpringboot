package com.phcpro.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeDTO(
    Long id,
    String employeeNumber,
    String name,
    String email,
    String phone,
    String taxId,
    String inssNumber,
    int dependentsCount,
    String department,
    BigDecimal baseSalary,
    String role,
    LocalDate hireDate,
    LocalDate contractEndDate,
    String status
) {}

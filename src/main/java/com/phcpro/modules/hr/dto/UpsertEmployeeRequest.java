package com.phcpro.modules.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertEmployeeRequest(
        @NotBlank String employeeNumber,
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String taxId,
        String inssNumber,
        @Min(0) int dependentsCount,
        @NotBlank String department,
        @NotBlank String role,
        @NotNull @PositiveOrZero BigDecimal baseSalary,
        @NotNull LocalDate hireDate,
        LocalDate contractEndDate
) {}

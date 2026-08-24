package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertEmployeeRequest(
        @NotBlank String employeeNumber,
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        @Size(max = 2_000_000, message = "A fotografia não pode exceder 2 MB.") byte[] photo,
        String taxId,
        String inssNumber,
        @Min(0) int dependentsCount,
        @NotBlank String department,
        @NotBlank String role,
        @NotNull @PositiveOrZero BigDecimal baseSalary,
        @NotNull LocalDate hireDate,
        LocalDate contractEndDate,
        /** Conta de utilizador do colaborador. Opcional: em branco desliga o self-service dele. */
        String username,
        /** Banco e conta para o ficheiro de pagamento (§B8.7). Em branco = recebe em numerário. */
        String bankName,
        String bankAccount
) {}

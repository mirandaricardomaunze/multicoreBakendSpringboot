package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeDTO(
    Long id,
    String employeeNumber,
    String name,
    String email,
    String phone,
    byte[] photo,
    String taxId,
    String inssNumber,
    int dependentsCount,
    String department,
    BigDecimal baseSalary,
    String role,
    LocalDate hireDate,
    LocalDate contractEndDate,
    String status,
    /** Conta de utilizador ligada, ou nulo. Nulo = este colaborador não faz self-service. */
    String username,
    /** Banco e conta para o ficheiro de pagamento (§B8.7). Nulos = recebe em numerário. */
    String bankName,
    String bankAccount
) {}

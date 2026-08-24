package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryChangeDTO(
        Long id,
        Long employeeId,
        String employeeName,
        BigDecimal previousSalary,
        BigDecimal newSalary,
        /** Diferença face ao anterior. Derivada — é a leitura que interessa a quem olha para a série. */
        BigDecimal difference,
        LocalDate effectiveDate,
        String reason,
        String reasonLabel,
        String jobTitle,
        String department,
        String approvedBy,
        String notes
) {}

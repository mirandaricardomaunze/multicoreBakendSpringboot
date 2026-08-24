package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato para fora da fronteira. {@code expired} e {@code daysUntilEnd} vêm <b>derivados</b> da
 * entidade contra a data de hoje — não são colunas. Ver {@code ContractStatus}.
 */
public record EmploymentContractDTO(
        Long id,
        String contractNumber,
        Long employeeId,
        String employeeName,
        String contractType,
        String contractTypeLabel,
        String status,
        String statusLabel,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate probationEndDate,
        BigDecimal agreedSalary,
        int weeklyHours,
        String jobTitle,
        String workLocation,
        String termReason,
        Long renewedFromId,
        String renewedFromNumber,
        LocalDate terminationDate,
        String terminationReason,
        boolean expired,
        boolean inProbation,
        Long daysUntilEnd
) {}

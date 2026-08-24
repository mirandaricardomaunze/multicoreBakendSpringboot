package mz.multicore.erp.modules.hr.dto;

import java.time.LocalDate;

/**
 * Um documento do colaborador e a sua validade. Ver docs/RH_COMPLETO_SPEC.md §B8.8.
 *
 * @param expiryDate      nulo = não caduca (NUIT, BI vitalício)
 * @param daysUntilExpiry negativo quando já caducou; nulo quando não caduca
 * @param expired         derivado da data contra hoje, nunca gravado
 */
public record EmployeeDocumentDTO(
        Long id,
        Long employeeId,
        String employeeName,
        String documentType,
        String documentNumber,
        LocalDate issueDate,
        LocalDate expiryDate,
        Long daysUntilExpiry,
        boolean expired,
        String notes
) {}

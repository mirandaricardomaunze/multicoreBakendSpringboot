package mz.multicore.erp.modules.hr.dto;

import java.time.LocalDate;

/**
 * Os valores legais em vigor numa empresa (§6 da RH_COMPLETO_SPEC). Campos nulos significam
 * <b>por confirmar com o contabilista</b>, e é isso que o painel mostra — não um zero.
 */
public record HrPolicyConfigDTO(
        Long id,
        String name,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Integer vacationDaysYear1,
        Integer vacationDaysYear2,
        Integer vacationDaysYear3Plus,
        Integer irpsDeliveryDay,
        Integer inssDeliveryDay,
        Integer noticeDaysEmployee,
        Integer noticeDaysEmployer,
        String legalBasis,
        boolean active
) {}

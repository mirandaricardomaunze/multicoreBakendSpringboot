package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget alert of a cost centre (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3): the actual costs (class 6) of the month
 * reached {@code thresholdPercent} (90 or 100) of the budgeted costs.
 *
 * @param budgeted budgeted costs of the centre in the month when the alert was raised
 * @param actual   actual costs of the centre in the month when the alert was raised
 * @param message  ready-to-show PT-MZ text for the bell
 */
public record BudgetAlertDTO(
        Long id,
        Long costCenterId,
        String costCenterCode,
        String costCenterName,
        int year,
        int month,
        int thresholdPercent,
        BigDecimal budgeted,
        BigDecimal actual,
        String message,
        LocalDateTime createdAt
) {}

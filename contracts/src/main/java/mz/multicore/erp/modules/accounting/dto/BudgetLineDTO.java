package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;

/** One cell of the annual budget (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1). */
public record BudgetLineDTO(
        String accountCode,
        String accountName,
        Long costCenterId,
        String costCenterCode,
        String costCenterName,
        int month,
        BigDecimal amount
) {}

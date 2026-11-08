package mz.multicore.erp.modules.fiscal.dto;

import java.math.BigDecimal;

/**
 * Result of a fiscal sales export in SAF-T format.
 * {@code xml} contains the complete file; totals allow verification without reparsing it.
 */
public record FiscalSalesExportDTO(
        String xml,
        int numberOfInvoices,
        BigDecimal totalNet,
        BigDecimal totalTax,
        BigDecimal totalGross
) {
}

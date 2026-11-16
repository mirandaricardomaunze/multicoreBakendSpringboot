package mz.multicore.erp.modules.projects.dto;

import java.math.BigDecimal;
import java.util.List;

/** Statement of one project: its orders, invoices and totals. */
public record ProjectStatementDTO(
        CustomerProjectDTO project,
        List<ProjectStatementLineDTO> lines,
        BigDecimal orderedTotal,
        BigDecimal invoicedTotal,
        BigDecimal outstanding
) {}

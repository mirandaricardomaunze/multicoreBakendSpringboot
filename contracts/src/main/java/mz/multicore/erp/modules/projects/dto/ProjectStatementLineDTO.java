package mz.multicore.erp.modules.projects.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One order of a project and, when billed, its invoice and what is still owed. */
public record ProjectStatementLineDTO(
        Long orderId,
        String orderNumber,
        LocalDate orderDate,
        String orderStatus,
        BigDecimal orderTotal,
        Long invoiceId,
        String invoiceNumber,
        BigDecimal invoiceTotal,
        BigDecimal outstanding
) {}

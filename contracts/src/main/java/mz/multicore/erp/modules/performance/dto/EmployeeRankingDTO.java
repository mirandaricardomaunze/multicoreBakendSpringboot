package mz.multicore.erp.modules.performance.dto;

import java.math.BigDecimal;

public record EmployeeRankingDTO(
        int rank,
        Long employeeId,
        String employeeName,
        BigDecimal revenue,
        BigDecimal grossMargin,
        BigDecimal avgTicket,
        int invoiceCount,
        BigDecimal goalProgressPct
) {}

package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeStatementDTO(
        LocalDate from,
        LocalDate to,
        List<FinancialStatementLineDTO> revenues,
        List<FinancialStatementLineDTO> expenses,
        BigDecimal totalRevenue,
        BigDecimal totalExpense,
        BigDecimal netResult
) {}

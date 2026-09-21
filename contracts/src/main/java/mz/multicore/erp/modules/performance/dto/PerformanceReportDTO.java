package mz.multicore.erp.modules.performance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PerformanceReportDTO(
        Long companyId,
        LocalDate from,
        LocalDate to,
        List<SalesGoalDTO> goals,
        List<SalesGoalProgressDTO> progresses,
        List<EmployeeRankingDTO> rankings,
        List<SalesGoalBonusDTO> bonuses,
        BigDecimal totalRevenue,
        BigDecimal totalMargin,
        BigDecimal totalBonuses
) {}

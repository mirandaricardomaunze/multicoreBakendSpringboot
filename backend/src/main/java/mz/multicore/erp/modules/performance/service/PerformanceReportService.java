package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.performance.dto.EmployeeRankingDTO;
import mz.multicore.erp.modules.performance.dto.PerformanceReportDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalBonusDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PerformanceReportService {

    private final SalesGoalService salesGoalService;
    private final EmployeeRankingService employeeRankingService;

    public PerformanceReportService(SalesGoalService salesGoalService,
                                   EmployeeRankingService employeeRankingService) {
        this.salesGoalService = salesGoalService;
        this.employeeRankingService = employeeRankingService;
    }

    @Transactional(readOnly = true)
    public PerformanceReportDTO generateReport(Long companyId, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = to != null ? to : LocalDate.now();

        List<SalesGoalDTO> goals = salesGoalService.listGoals(companyId, null, null);
        List<SalesGoalProgressDTO> progresses = salesGoalService.getActiveGoalsSummary(companyId);
        List<EmployeeRankingDTO> rankings = employeeRankingService.getRanking(companyId, startDate, endDate);
        List<SalesGoalBonusDTO> bonuses = salesGoalService.listBonuses(companyId, null);

        BigDecimal totalRevenue = rankings.stream()
                .map(EmployeeRankingDTO::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMargin = rankings.stream()
                .map(EmployeeRankingDTO::grossMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBonuses = bonuses.stream()
                .map(SalesGoalBonusDTO::approvedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PerformanceReportDTO(
                companyId,
                startDate,
                endDate,
                goals,
                progresses,
                rankings,
                bonuses,
                totalRevenue,
                totalMargin,
                totalBonuses
        );
    }
}

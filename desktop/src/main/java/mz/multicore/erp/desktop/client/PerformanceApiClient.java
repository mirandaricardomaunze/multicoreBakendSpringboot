package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.performance.dto.*;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("desktop")
public class PerformanceApiClient {

    private final DesktopClientFactory clientFactory;

    public PerformanceApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<SalesGoalDTO> getGoals(Long companyId, GoalStatus status, GoalPeriod period) {
        StringBuilder sb = new StringBuilder("/api/performance/goals?");
        if (companyId != null) sb.append("companyId=").append(companyId).append("&");
        if (status != null) sb.append("status=").append(status.name()).append("&");
        if (period != null) sb.append("period=").append(period.name()).append("&");
        return clientFactory.authenticatedClient().getList(sb.toString(), SalesGoalDTO.class);
    }

    public SalesGoalDTO getGoal(Long id) {
        return clientFactory.authenticatedClient().get("/api/performance/goals/" + id, SalesGoalDTO.class);
    }

    public SalesGoalDTO createGoal(CreateSalesGoalRequest request) {
        return clientFactory.authenticatedClient().post("/api/performance/goals", request, SalesGoalDTO.class);
    }

    public SalesGoalDTO updateGoal(Long id, UpdateSalesGoalRequest request) {
        return clientFactory.authenticatedClient().put("/api/performance/goals/" + id, request, SalesGoalDTO.class);
    }

    public void cancelGoal(Long id) {
        clientFactory.authenticatedClient().delete("/api/performance/goals/" + id);
    }

    public SalesGoalProgressDTO getGoalProgress(Long id) {
        return clientFactory.authenticatedClient().get("/api/performance/goals/" + id + "/progress", SalesGoalProgressDTO.class);
    }

    public List<SalesGoalProgressDTO> getActiveGoalsSummary(Long companyId) {
        String path = "/api/performance/goals/active-summary" + (companyId != null ? "?companyId=" + companyId : "");
        return clientFactory.authenticatedClient().getList(path, SalesGoalProgressDTO.class);
    }

    public List<EmployeeRankingDTO> getRanking(Long companyId, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("/api/performance/ranking?");
        if (companyId != null) sb.append("companyId=").append(companyId).append("&");
        if (from != null) sb.append("from=").append(from).append("&");
        if (to != null) sb.append("to=").append(to).append("&");
        return clientFactory.authenticatedClient().getList(sb.toString(), EmployeeRankingDTO.class);
    }

    public List<SalesGoalBonusDTO> getBonuses(Long companyId, BonusStatus status) {
        StringBuilder sb = new StringBuilder("/api/performance/bonuses?");
        if (companyId != null) sb.append("companyId=").append(companyId).append("&");
        if (status != null) sb.append("status=").append(status.name()).append("&");
        return clientFactory.authenticatedClient().getList(sb.toString(), SalesGoalBonusDTO.class);
    }

    public SalesGoalBonusDTO approveBonus(Long id, ApproveBonusRequest request) {
        return clientFactory.authenticatedClient().post("/api/performance/bonuses/" + id + "/approve", request, SalesGoalBonusDTO.class);
    }

    public SalesGoalBonusDTO adjustBonus(Long id, AdjustBonusRequest request) {
        return clientFactory.authenticatedClient().post("/api/performance/bonuses/" + id + "/adjust", request, SalesGoalBonusDTO.class);
    }

    public SalesGoalBonusDTO integrateBonusWithPayroll(Long id, int year, int month) {
        return clientFactory.authenticatedClient().post("/api/performance/bonuses/" + id + "/integrate-payroll?year=" + year + "&month=" + month, null, SalesGoalBonusDTO.class);
    }

    public PerformanceReportDTO getReport(Long companyId, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("/api/performance/report?");
        if (companyId != null) sb.append("companyId=").append(companyId).append("&");
        if (from != null) sb.append("from=").append(from).append("&");
        if (to != null) sb.append("to=").append(to).append("&");
        return clientFactory.authenticatedClient().get(sb.toString(), PerformanceReportDTO.class);
    }

    public byte[] printReport(Long companyId, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("/api/print/performance-report?companyId=").append(companyId);
        if (from != null) sb.append("&from=").append(from);
        if (to != null) sb.append("&to=").append(to);
        return clientFactory.authenticatedClient().getBytes(sb.toString());
    }
}

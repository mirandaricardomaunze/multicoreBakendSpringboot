package mz.multicore.erp.modules.performance.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.performance.dto.*;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.service.EmployeeRankingService;
import mz.multicore.erp.modules.performance.service.PerformanceReportService;
import mz.multicore.erp.modules.performance.service.SalesGoalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final SalesGoalService salesGoalService;
    private final EmployeeRankingService employeeRankingService;
    private final PerformanceReportService performanceReportService;

    public PerformanceController(SalesGoalService salesGoalService,
                                 EmployeeRankingService employeeRankingService,
                                 PerformanceReportService performanceReportService) {
        this.salesGoalService = salesGoalService;
        this.employeeRankingService = employeeRankingService;
        this.performanceReportService = performanceReportService;
    }

    private Long resolveCompanyId(Long companyId) {
        return companyId != null ? companyId : CurrentUserContext.getCurrentCompanyId();
    }

    @GetMapping("/goals")
    public ResponseEntity<List<SalesGoalDTO>> listGoals(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) GoalStatus status,
            @RequestParam(required = false) GoalPeriod period) {
        Long cid = resolveCompanyId(companyId);
        return ResponseEntity.ok(salesGoalService.listGoals(cid, status, period));
    }

    @GetMapping("/goals/{id}")
    public ResponseEntity<SalesGoalDTO> getGoal(
            @PathVariable Long id,
            @RequestParam(required = false) Long companyId) {
        Long cid = resolveCompanyId(companyId);
        return ResponseEntity.ok(salesGoalService.getGoal(id, cid));
    }

    @PostMapping("/goals")
    public ResponseEntity<SalesGoalDTO> createGoal(@Valid @RequestBody CreateSalesGoalRequest request) {
        CreateSalesGoalRequest enriched = request.companyId() != null
                ? request
                : new CreateSalesGoalRequest(
                        CurrentUserContext.getCurrentCompanyId(),
                        request.name(),
                        request.period(),
                        request.periodStart(),
                        request.periodEnd(),
                        request.scope(),
                        request.scopeRefId(),
                        request.scopeLabel(),
                        request.targetRevenue(),
                        request.targetMargin(),
                        request.targetMarginPct(),
                        request.bonusType(),
                        request.bonusValue(),
                        request.bonusCap(),
                        request.autoApplyBonus());
        return ResponseEntity.status(HttpStatus.CREATED).body(salesGoalService.createGoal(enriched));
    }

    @PutMapping("/goals/{id}")
    public ResponseEntity<SalesGoalDTO> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalesGoalRequest request) {
        return ResponseEntity.ok(salesGoalService.updateGoal(id, request));
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<Void> cancelGoal(@PathVariable Long id) {
        salesGoalService.cancelGoal(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goals/{id}/progress")
    public ResponseEntity<SalesGoalProgressDTO> getGoalProgress(@PathVariable Long id) {
        return ResponseEntity.ok(salesGoalService.getGoalProgress(id));
    }

    @GetMapping("/goals/active-summary")
    public ResponseEntity<List<SalesGoalProgressDTO>> getActiveGoalsSummary(
            @RequestParam(required = false) Long companyId) {
        Long cid = resolveCompanyId(companyId);
        return ResponseEntity.ok(salesGoalService.getActiveGoalsSummary(cid));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<EmployeeRankingDTO>> getRanking(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long cid = resolveCompanyId(companyId);
        return ResponseEntity.ok(employeeRankingService.getRanking(cid, from, to));
    }

    @GetMapping("/bonuses")
    public ResponseEntity<List<SalesGoalBonusDTO>> listBonuses(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) BonusStatus status) {
        Long cid = resolveCompanyId(companyId);
        return ResponseEntity.ok(salesGoalService.listBonuses(cid, status));
    }

    @PostMapping("/bonuses/{id}/approve")
    public ResponseEntity<SalesGoalBonusDTO> approveBonus(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid ApproveBonusRequest request) {
        return ResponseEntity.ok(salesGoalService.approveBonus(id, request));
    }

    @PostMapping("/bonuses/{id}/adjust")
    public ResponseEntity<SalesGoalBonusDTO> adjustBonus(
            @PathVariable Long id,
            @Valid @RequestBody AdjustBonusRequest request) {
        return ResponseEntity.ok(salesGoalService.adjustBonus(id, request));
    }

    @PostMapping("/bonuses/{id}/integrate-payroll")
    public ResponseEntity<SalesGoalBonusDTO> integratePayroll(
            @PathVariable Long id,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(salesGoalService.integrateBonusWithPayroll(id, year, month));
    }

    @GetMapping("/report")
    public ResponseEntity<PerformanceReportDTO> getReport(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long cid = resolveCompanyId(companyId);
        return ResponseEntity.ok(performanceReportService.generateReport(cid, from, to));
    }
}

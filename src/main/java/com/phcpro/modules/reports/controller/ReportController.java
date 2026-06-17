package com.phcpro.modules.reports.controller;

import com.phcpro.modules.reports.dto.DailyStoreReportDTO;
import com.phcpro.modules.reports.dto.StoreDashboardDTO;
import com.phcpro.modules.reports.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/store-dashboard")
    public ResponseEntity<StoreDashboardDTO> storeDashboard(@RequestParam Long companyId) {
        return ResponseEntity.ok(reportService.buildStoreDashboard(companyId));
    }

    @GetMapping("/daily-store")
    public ResponseEntity<DailyStoreReportDTO> dailyStoreReport(
            @RequestParam Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(reportService.buildDailyStoreReport(companyId, date));
    }
}

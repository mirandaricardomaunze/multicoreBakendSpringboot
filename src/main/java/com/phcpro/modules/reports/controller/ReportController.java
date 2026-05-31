package com.phcpro.modules.reports.controller;

import com.phcpro.modules.reports.dto.StoreDashboardDTO;
import com.phcpro.modules.reports.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

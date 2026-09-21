package mz.multicore.erp.modules.audit.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import mz.multicore.erp.modules.audit.service.ForensicAuditService;
import mz.multicore.erp.modules.printing.ForensicAuditPrintService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controlador REST da Central de Auditoria Forense e Controlo de Fraude Interna.
 */
@RestController
@RequestMapping("/api/audit/forensic")
public class ForensicAuditController {

    private final ForensicAuditService forensicAuditService;
    private final ForensicAuditPrintService forensicAuditPrintService;

    public ForensicAuditController(
            ForensicAuditService forensicAuditService,
            ForensicAuditPrintService forensicAuditPrintService
    ) {
        this.forensicAuditService = forensicAuditService;
        this.forensicAuditPrintService = forensicAuditPrintService;
    }

    @GetMapping
    public ResponseEntity<ForensicAuditSummaryDTO> getForensicSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ForensicSeverity severity,
            @RequestParam(required = false) ForensicCategory category,
            @RequestParam(required = false) String operator
    ) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        ForensicAuditSummaryDTO summary = forensicAuditService.getForensicSummaryForCompany(
                companyId, startDate, endDate, severity, category, operator);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> getForensicPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ForensicSeverity severity,
            @RequestParam(required = false) ForensicCategory category,
            @RequestParam(required = false) String operator
    ) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        byte[] pdfBytes = forensicAuditPrintService.render(
                companyId, startDate, endDate, severity, category, operator);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"dossie-auditoria-forense.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

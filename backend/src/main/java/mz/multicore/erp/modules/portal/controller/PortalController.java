package mz.multicore.erp.modules.portal.controller;

import mz.multicore.erp.modules.portal.dto.PortalSummaryDTO;
import mz.multicore.erp.modules.portal.service.PortalSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Leituras do portal web do gestor (PORTAL_GESTOR_SPEC §3.3); nada se altera por aqui. */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalSummaryService summaryService;

    public PortalController(PortalSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public ResponseEntity<PortalSummaryDTO> summary(@RequestParam Long companyId) {
        return ResponseEntity.ok(summaryService.buildSummary(companyId));
    }
}

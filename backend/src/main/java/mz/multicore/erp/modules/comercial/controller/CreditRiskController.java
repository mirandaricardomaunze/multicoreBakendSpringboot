package mz.multicore.erp.modules.comercial.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import mz.multicore.erp.modules.comercial.service.CreditRiskService;
import mz.multicore.erp.modules.printing.DebtCollectionNoticePrintService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint executivo de risco de crédito, matriz de aging e notificações de cobrança.
 */
@RestController
@RequestMapping("/api/comercial/credit-risk")
public class CreditRiskController {

    private final CreditRiskService creditRiskService;
    private final DebtCollectionNoticePrintService noticePrintService;

    public CreditRiskController(
            CreditRiskService creditRiskService,
            DebtCollectionNoticePrintService noticePrintService
    ) {
        this.creditRiskService = creditRiskService;
        this.noticePrintService = noticePrintService;
    }

    @GetMapping("/summary")
    public ResponseEntity<CreditRiskSummaryDTO> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reference) {
        return ResponseEntity.ok(creditRiskService.getSummary(reference));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientCreditRiskDTO>> getClientsRisk(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reference) {
        return ResponseEntity.ok(creditRiskService.getClientsRisk(reference));
    }

    @GetMapping("/notice")
    public ResponseEntity<DebtCollectionNoticeDTO> getNotice(
            @RequestParam Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reference) {
        return ResponseEntity.ok(creditRiskService.getDebtCollectionNotice(clientId, reference));
    }

    @GetMapping("/notice/pdf")
    public ResponseEntity<byte[]> getNoticePdf(
            @RequestParam Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reference) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        byte[] pdfBytes = noticePrintService.render(companyId, clientId, reference);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"notificacao-cobranca-" + clientId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/exception-request")
    public ResponseEntity<ApprovalRequestDTO> requestCreditException(
            @RequestBody CreditExceptionApprovalRequest request) {
        return ResponseEntity.ok(creditRiskService.requestCreditException(request));
    }
}

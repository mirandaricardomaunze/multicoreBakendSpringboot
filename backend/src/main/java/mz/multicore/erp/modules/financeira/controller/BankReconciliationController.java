package mz.multicore.erp.modules.financeira.controller;

import jakarta.validation.Valid;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.financeira.dto.BankReconciliationSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.dto.CreateBankExpenseAndMatchRequest;
import mz.multicore.erp.modules.financeira.dto.ImportBankStatementRequest;
import mz.multicore.erp.modules.financeira.dto.ManualReconciliationRequest;
import mz.multicore.erp.modules.financeira.service.BankReconciliationService;
import mz.multicore.erp.modules.printing.BankReconciliationPrintService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/reconciliation")
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;
    private final BankReconciliationPrintService printService;

    public BankReconciliationController(
            BankReconciliationService reconciliationService,
            BankReconciliationPrintService printService
    ) {
        this.reconciliationService = reconciliationService;
        this.printService = printService;
    }

    @PostMapping("/statements/import")
    public ResponseEntity<BankStatementDTO> importStatement(@RequestBody @Valid ImportBankStatementRequest request) {
        return ResponseEntity.ok(reconciliationService.importStatement(request));
    }

    @GetMapping("/statements")
    public ResponseEntity<List<BankStatementDTO>> getStatements(
            @RequestParam(required = false) Long accountId) {
        return ResponseEntity.ok(reconciliationService.getStatements(accountId));
    }

    @GetMapping("/statements/{id}")
    public ResponseEntity<BankStatementDTO> getStatement(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getStatement(id));
    }

    @GetMapping("/statements/{id}/items")
    public ResponseEntity<List<BankStatementItemDTO>> getStatementItems(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getStatementItems(id));
    }

    @PostMapping("/statements/{id}/auto-match")
    public ResponseEntity<Map<String, Object>> autoMatch(@PathVariable Long id) {
        int matched = reconciliationService.autoMatch(id);
        return ResponseEntity.ok(Map.of("matchedCount", matched));
    }

    @PostMapping("/items/match")
    public ResponseEntity<BankStatementItemDTO> manualMatch(@RequestBody @Valid ManualReconciliationRequest request) {
        return ResponseEntity.ok(reconciliationService.manualMatch(request.statementItemId(), request.treasuryTransactionId()));
    }

    @PostMapping("/items/{id}/unmatch")
    public ResponseEntity<BankStatementItemDTO> unmatch(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.unmatch(id));
    }

    @PostMapping("/items/expense")
    public ResponseEntity<BankStatementItemDTO> createAndMatchExpense(@RequestBody @Valid CreateBankExpenseAndMatchRequest request) {
        return ResponseEntity.ok(reconciliationService.createAndMatchExpense(
                request.statementItemId(), request.description(), request.amount()));
    }

    @PostMapping("/statements/{id}/close")
    public ResponseEntity<BankStatementDTO> closeStatement(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.closeStatement(id));
    }

    @GetMapping("/summary")
    public ResponseEntity<BankReconciliationSummaryDTO> getSummary(@RequestParam Long accountId) {
        return ResponseEntity.ok(reconciliationService.getSummary(accountId));
    }

    @GetMapping("/statements/{id}/pdf")
    public ResponseEntity<byte[]> renderReportPdf(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        byte[] pdfBytes = printService.render(companyId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"reconciliacao-bancaria-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

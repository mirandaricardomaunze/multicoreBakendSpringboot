package mz.multicore.erp.modules.financeira.controller;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.financeira.dto.*;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<TreasuryAccountDTO>> getAccounts() {
        return ResponseEntity.ok(financeService.getAllAccounts());
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TreasuryTransactionDTO>> getTransactions() {
        return ResponseEntity.ok(financeService.getAllTransactions());
    }

    @GetMapping("/transactions/page")
    public ResponseEntity<PageResponse<TreasuryTransactionDTO>> getTransactionsPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        return ResponseEntity.ok(financeService.getTransactionsPage(page, size));
    }

    @PostMapping("/pay-invoice")
    public ResponseEntity<Void> payInvoice(@RequestBody @Valid PayInvoiceRequest request) {
        financeService.payInvoice(request.invoiceId(), request.accountId());
        return ResponseEntity.ok().build();
    }
}

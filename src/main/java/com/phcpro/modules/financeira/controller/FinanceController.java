package com.phcpro.modules.financeira.controller;

import com.phcpro.modules.financeira.dto.*;
import com.phcpro.modules.financeira.service.FinanceService;
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

    @PostMapping("/pay-invoice")
    public ResponseEntity<Void> payInvoice(@RequestBody @Valid PayInvoiceRequest request) {
        financeService.payInvoice(request.invoiceId(), request.accountId());
        return ResponseEntity.ok().build();
    }
}

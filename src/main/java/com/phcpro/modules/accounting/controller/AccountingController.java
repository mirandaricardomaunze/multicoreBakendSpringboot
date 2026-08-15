package com.phcpro.modules.accounting.controller;

import com.phcpro.architecture.paging.PageResponse;
import com.phcpro.modules.accounting.dto.*;
import com.phcpro.modules.accounting.service.AccountingReportService;
import com.phcpro.modules.accounting.service.ChartOfAccountsService;
import com.phcpro.modules.accounting.service.JournalService;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Contabilidade: plano de contas, diário, razão e balancete. Só HTTP. */
@RestController
@RequestMapping("/api/accounting")
public class AccountingController {

    private final ChartOfAccountsService chartOfAccountsService;
    private final JournalService journalService;
    private final AccountingReportService accountingReportService;

    public AccountingController(ChartOfAccountsService chartOfAccountsService,
                                JournalService journalService,
                                AccountingReportService accountingReportService) {
        this.chartOfAccountsService = chartOfAccountsService;
        this.journalService = journalService;
        this.accountingReportService = accountingReportService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDTO>> getAccounts() {
        return ResponseEntity.ok(chartOfAccountsService.listAccounts());
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountDTO> createAccount(@RequestBody @Valid SaveAccountRequest request) {
        return ResponseEntity.ok(chartOfAccountsService.createAccount(request));
    }

    /** Semeia o PGC-NIRF. Idempotente: devolve {@code created = 0} se já houver plano. */
    @PostMapping("/accounts/seed")
    public ResponseEntity<Map<String, Integer>> seedChart() {
        return ResponseEntity.ok(Map.of("created", chartOfAccountsService.seedDefaultChart()));
    }

    @GetMapping("/journal")
    public ResponseEntity<PageResponse<JournalEntryDTO>> getJournal(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(journalService.getJournalPage(page, size));
    }

    @PostMapping("/journal")
    public ResponseEntity<JournalEntryDTO> createEntry(@RequestBody @Valid CreateJournalEntryRequest request) {
        return ResponseEntity.ok(journalService.createManualEntry(request));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceDTO> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(accountingReportService.getTrialBalance(from, to));
    }

    @GetMapping("/ledger/{accountCode}")
    public ResponseEntity<LedgerDTO> getLedger(
            @PathVariable String accountCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(accountingReportService.getLedger(accountCode, from, to));
    }
}

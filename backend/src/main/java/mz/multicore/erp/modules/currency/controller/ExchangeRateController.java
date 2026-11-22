package mz.multicore.erp.modules.currency.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.currency.dto.CorrectExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.CreateExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;
import mz.multicore.erp.modules.currency.dto.ProposedExchangeRateDTO;
import mz.multicore.erp.modules.currency.service.ExchangeRateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily exchange-rate table of the active company (SPEC §3.2). Creating and correcting is ADMIN only and
 * consulting is for whoever registers purchases; both decided in {@code ApiRolePolicy}.
 */
@RestController
@RequestMapping("/api/exchange-rates")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    public ResponseEntity<List<ExchangeRateDTO>> list() {
        return ResponseEntity.ok(exchangeRateService.list(CurrentUserContext.requireCurrentCompanyId()));
    }

    /** Rate proposed for a document date: that date or the most recent earlier one. */
    @GetMapping("/proposal")
    public ResponseEntity<ProposedExchangeRateDTO> propose(
            @RequestParam String currency,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.propose(CurrentUserContext.requireCurrentCompanyId(), currency, date));
    }

    @PostMapping
    public ResponseEntity<ExchangeRateDTO> create(@Valid @RequestBody CreateExchangeRateRequest request) {
        return ResponseEntity.ok(exchangeRateService.create(CurrentUserContext.requireCurrentCompanyId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExchangeRateDTO> correct(@PathVariable Long id,
                                                   @Valid @RequestBody CorrectExchangeRateRequest request) {
        return ResponseEntity.ok(exchangeRateService.correct(CurrentUserContext.requireCurrentCompanyId(), id, request));
    }
}

package com.phcpro.modules.fiscal.controller;

import com.phcpro.modules.fiscal.dto.CreateTaxRateRequest;
import com.phcpro.modules.fiscal.dto.CreateWithholdingRequest;
import com.phcpro.modules.fiscal.dto.IvaSummaryDTO;
import com.phcpro.modules.fiscal.dto.TaxRateDTO;
import com.phcpro.modules.fiscal.dto.WithholdingRecordDTO;
import com.phcpro.modules.fiscal.service.FiscalSummaryService;
import com.phcpro.modules.fiscal.service.TaxRateService;
import com.phcpro.modules.fiscal.service.WithholdingService;
import com.phcpro.modules.hr.dto.PayrollFiscalSummaryDTO;
import com.phcpro.modules.hr.dto.PayrollTaxConfigDTO;
import com.phcpro.modules.hr.dto.SavePayrollTaxConfigRequest;
import com.phcpro.modules.hr.service.PayrollTaxConfigService;
import com.phcpro.modules.hr.service.PayrollTaxService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fiscal")
public class FiscalController {

    private final TaxRateService taxRateService;
    private final WithholdingService withholdingService;
    private final FiscalSummaryService fiscalSummaryService;
    private final PayrollTaxService payrollTaxService;
    private final PayrollTaxConfigService payrollTaxConfigService;

    public FiscalController(TaxRateService taxRateService,
                            WithholdingService withholdingService,
                            FiscalSummaryService fiscalSummaryService,
                            PayrollTaxService payrollTaxService,
                            PayrollTaxConfigService payrollTaxConfigService) {
        this.taxRateService = taxRateService;
        this.withholdingService = withholdingService;
        this.fiscalSummaryService = fiscalSummaryService;
        this.payrollTaxService = payrollTaxService;
        this.payrollTaxConfigService = payrollTaxConfigService;
    }

    @GetMapping("/payroll/{year}/{month}")
    public ResponseEntity<PayrollFiscalSummaryDTO> payrollSummary(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(payrollTaxService.fiscalSummary(year, month));
    }

    @GetMapping("/payroll/configs")
    public ResponseEntity<List<PayrollTaxConfigDTO>> payrollConfigs() {
        return ResponseEntity.ok(payrollTaxConfigService.list());
    }

    @PostMapping("/payroll/configs")
    public ResponseEntity<PayrollTaxConfigDTO> createPayrollConfig(
            @RequestBody @Valid SavePayrollTaxConfigRequest request) {
        return ResponseEntity.ok(payrollTaxConfigService.create(request));
    }

    // ── Taxas Fiscais ────────────────────────────────────────────────────

    @GetMapping("/tax-rates")
    public ResponseEntity<List<TaxRateDTO>> listTaxRates(
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(onlyActive ? taxRateService.getActive() : taxRateService.getAll());
    }

    @PostMapping("/tax-rates")
    public ResponseEntity<TaxRateDTO> createTaxRate(@RequestBody @Valid CreateTaxRateRequest request) {
        return ResponseEntity.ok(taxRateService.create(request));
    }

    @PutMapping("/tax-rates/{id}")
    public ResponseEntity<TaxRateDTO> updateTaxRate(@PathVariable Long id,
                                                     @RequestBody @Valid CreateTaxRateRequest request) {
        return ResponseEntity.ok(taxRateService.update(id, request));
    }

    @PostMapping("/tax-rates/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        taxRateService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tax-rates/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        taxRateService.activate(id);
        return ResponseEntity.noContent().build();
    }

    // ── Retenções na Fonte ───────────────────────────────────────────────

    @GetMapping("/withholdings")
    public ResponseEntity<List<WithholdingRecordDTO>> listWithholdings(@RequestParam Long companyId) {
        return ResponseEntity.ok(withholdingService.findByCompany(companyId));
    }

    @PostMapping("/withholdings")
    public ResponseEntity<WithholdingRecordDTO> create(@RequestBody @Valid CreateWithholdingRequest request) {
        return ResponseEntity.ok(withholdingService.create(request));
    }

    @PostMapping("/withholdings/{id}/deliver")
    public ResponseEntity<WithholdingRecordDTO> deliver(@PathVariable Long id) {
        return ResponseEntity.ok(withholdingService.markDelivered(id));
    }

    @DeleteMapping("/withholdings/{id}")
    public ResponseEntity<Void> deleteWithholding(@PathVariable Long id) {
        withholdingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Apuramento IVA ───────────────────────────────────────────────────

    @GetMapping("/iva-summary")
    public ResponseEntity<IvaSummaryDTO> ivaSummary(
            @RequestParam Long companyId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(fiscalSummaryService.computeMonth(companyId, year, month));
    }
}

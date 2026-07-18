package com.phcpro.desktop.client;

import com.phcpro.modules.fiscal.dto.CreateTaxRateRequest;
import com.phcpro.modules.fiscal.dto.CreateWithholdingRequest;
import com.phcpro.modules.fiscal.dto.FiscalSalesExportDTO;
import com.phcpro.modules.fiscal.dto.IvaSummaryDTO;
import com.phcpro.modules.fiscal.dto.SaftValidationResult;
import com.phcpro.modules.fiscal.dto.TaxRateDTO;
import com.phcpro.modules.fiscal.dto.WithholdingRecordDTO;
import com.phcpro.modules.hr.dto.PayrollFiscalSummaryDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP para a área fiscal ({@code /api/fiscal}) + impressões fiscais ({@code /api/print}).
 * Colapsa num só cliente os vários serviços que o {@code FiscalPanel} usava em processo (taxas,
 * retenções, apuramento IVA, mapa salarial, SAF-T). Espelha o padrão do {@link ComercialApiClient}.
 */
@Component
@Profile("desktop")
public class FiscalApiClient {

    private final DesktopClientFactory clientFactory;

    public FiscalApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    // ─── Mapa fiscal salarial (INSS/IRPS) ────────────────────────────────────
    public PayrollFiscalSummaryDTO fiscalSummary(int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/fiscal/payroll/" + year + "/" + month, PayrollFiscalSummaryDTO.class);
    }

    // ─── Apuramento do IVA ───────────────────────────────────────────────────
    public IvaSummaryDTO ivaSummary(Long companyId, int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/fiscal/iva-summary?companyId=" + companyId + "&year=" + year + "&month=" + month,
                        IvaSummaryDTO.class);
    }

    // ─── Taxas fiscais ───────────────────────────────────────────────────────
    public List<TaxRateDTO> getAllTaxRates() {
        return clientFactory.authenticatedClient().getList("/api/fiscal/tax-rates", TaxRateDTO.class);
    }

    public TaxRateDTO createTaxRate(CreateTaxRateRequest request) {
        return clientFactory.authenticatedClient().post("/api/fiscal/tax-rates", request, TaxRateDTO.class);
    }

    public TaxRateDTO updateTaxRate(Long id, CreateTaxRateRequest request) {
        return clientFactory.authenticatedClient().put("/api/fiscal/tax-rates/" + id, request, TaxRateDTO.class);
    }

    public void deactivateTaxRate(Long id) {
        clientFactory.authenticatedClient().post("/api/fiscal/tax-rates/" + id + "/deactivate", null);
    }

    public void activateTaxRate(Long id) {
        clientFactory.authenticatedClient().post("/api/fiscal/tax-rates/" + id + "/activate", null);
    }

    // ─── Retenções na fonte ──────────────────────────────────────────────────
    public List<WithholdingRecordDTO> getWithholdings(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/fiscal/withholdings?companyId=" + companyId, WithholdingRecordDTO.class);
    }

    public WithholdingRecordDTO createWithholding(CreateWithholdingRequest request) {
        return clientFactory.authenticatedClient().post("/api/fiscal/withholdings", request, WithholdingRecordDTO.class);
    }

    public WithholdingRecordDTO deliverWithholding(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/fiscal/withholdings/" + id + "/deliver", null, WithholdingRecordDTO.class);
    }

    public void deleteWithholding(Long id) {
        clientFactory.authenticatedClient().delete("/api/fiscal/withholdings/" + id);
    }

    // ─── SAF-T (vendas) ──────────────────────────────────────────────────────
    public FiscalSalesExportDTO exportSaft(Long companyId, LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient()
                .get("/api/fiscal/saft/export?companyId=" + companyId + "&from=" + from + "&to=" + to,
                        FiscalSalesExportDTO.class);
    }

    public SaftValidationResult validateSaft(Long companyId, LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient()
                .get("/api/fiscal/saft/validate?companyId=" + companyId + "&from=" + from + "&to=" + to,
                        SaftValidationResult.class);
    }

    // ─── Impressões fiscais (PDF) ─────────────────────────────────────────────
    public byte[] renderIvaDeclaration(Long companyId, int year, int month) {
        return clientFactory.authenticatedClient()
                .getBytes("/api/print/iva-declaration?companyId=" + companyId + "&year=" + year + "&month=" + month);
    }

    public byte[] renderPayrollFiscalMap(Long companyId, int year, int month) {
        return clientFactory.authenticatedClient()
                .getBytes("/api/print/payroll-fiscal-map?companyId=" + companyId + "&year=" + year + "&month=" + month);
    }
}

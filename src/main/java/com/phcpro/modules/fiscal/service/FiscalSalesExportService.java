package com.phcpro.modules.fiscal.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.fiscal.dto.FiscalSalesExportDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Exporta os documentos de venda de um período num XML alinhado com a estrutura SAF-T
 * (Header / MasterFiles / SourceDocuments). Ver {@code docs/FISCAL_SAFT_EXPORT_SPEC.md}.
 *
 * <p><b>Limite:</b> segue a estrutura SAF-T mas não é certificado — validar contra a XSD oficial
 * da AT-MZ antes de submissão. Reutiliza os valores fiscais já persistidos na fatura (não recalcula
 * impostos, para não divergir da engine de faturação/POS).
 */
@Service
public class FiscalSalesExportService {

    /** Estados que representam documentos fiscais emitidos (entram no ficheiro). */
    private static final Set<InvoiceStatus> ISSUED = EnumSet.of(
            InvoiceStatus.APPROVED,
            InvoiceStatus.PAID,
            InvoiceStatus.PARTIALLY_PAID,
            InvoiceStatus.CANCELLED);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;

    public FiscalSalesExportService(InvoiceRepository invoiceRepository,
                                    CompanyRepository companyRepository) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public FiscalSalesExportDTO exportSales(Long companyId, LocalDate from, LocalDate to) {
        PermissionGuard.requireManagerOrAdmin("exportar ficheiro fiscal de vendas");
        CurrentUserContext.requireCompany(companyId);
        if (from == null || to == null) {
            throw new BusinessRuleException("Indique o período (datas de início e fim) da exportação.");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("A data final não pode ser anterior à data inicial.");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(i -> ISSUED.contains(i.getStatus()))
                .filter(i -> withinPeriod(i.getCreatedAt(), start, end))
                .sorted(Comparator.comparing(Invoice::getInvoiceNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        Map<Long, Client> customers = new LinkedHashMap<>();
        Set<BigDecimal> taxRates = new TreeSet<>();

        for (Invoice inv : invoices) {
            boolean cancelled = inv.getStatus() == InvoiceStatus.CANCELLED;
            if (!cancelled) {
                totalNet = totalNet.add(nz(inv.getTotalBeforeTax()));
                totalTax = totalTax.add(nz(inv.getTaxAmount()));
                totalGross = totalGross.add(nz(inv.getTotalAmount()));
            }
            Client c = inv.getClient();
            if (c != null) {
                customers.putIfAbsent(c.getId(), c);
            }
            for (InvoiceLine line : inv.getLines()) {
                taxRates.add(percent(line.getTaxRate()));
            }
        }

        String xml = buildXml(company, from, to, invoices, customers.values(), taxRates, totalNet);
        return new FiscalSalesExportDTO(xml, invoices.size(),
                scale(totalNet), scale(totalTax), scale(totalGross));
    }

    // ─── Construção do XML ───────────────────────────────────────────────────

    private String buildXml(Company company, LocalDate from, LocalDate to,
                            List<Invoice> invoices, Iterable<Client> customers,
                            Set<BigDecimal> taxRates, BigDecimal totalCredit) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<AuditFile>\n");

        sb.append("  <Header>\n");
        tag(sb, 2, "CompanyName", company.getName());
        tag(sb, 2, "TaxRegistrationNumber", company.getTaxId());
        tag(sb, 2, "FiscalYear", String.valueOf(from.getYear()));
        tag(sb, 2, "StartDate", from.format(DATE));
        tag(sb, 2, "EndDate", to.format(DATE));
        tag(sb, 2, "CurrencyCode", "MZN");
        tag(sb, 2, "DateCreated", LocalDate.now().format(DATE));
        tag(sb, 2, "ProductID", "Multicore ERP");
        sb.append("  </Header>\n");

        sb.append("  <MasterFiles>\n");
        for (Client c : customers) {
            sb.append("    <Customer>\n");
            tag(sb, 3, "CustomerID", String.valueOf(c.getId()));
            tag(sb, 3, "CompanyName", c.getName());
            tag(sb, 3, "TaxRegistrationNumber", c.getTaxId());
            sb.append("    </Customer>\n");
        }
        for (BigDecimal rate : taxRates) {
            sb.append("    <TaxTableEntry>\n");
            tag(sb, 3, "TaxType", "IVA");
            tag(sb, 3, "TaxPercentage", money(rate));
            sb.append("    </TaxTableEntry>\n");
        }
        sb.append("  </MasterFiles>\n");

        sb.append("  <SourceDocuments>\n");
        sb.append("    <SalesInvoices>\n");
        tag(sb, 3, "NumberOfEntries", String.valueOf(invoices.size()));
        tag(sb, 3, "TotalDebit", money(BigDecimal.ZERO));
        tag(sb, 3, "TotalCredit", money(totalCredit));
        for (Invoice inv : invoices) {
            appendInvoice(sb, inv);
        }
        sb.append("    </SalesInvoices>\n");
        sb.append("  </SourceDocuments>\n");

        sb.append("</AuditFile>\n");
        return sb.toString();
    }

    private void appendInvoice(StringBuilder sb, Invoice inv) {
        sb.append("      <Invoice>\n");
        tag(sb, 4, "InvoiceNo", inv.getInvoiceNumber());
        tag(sb, 4, "InvoiceDate", inv.getCreatedAt() == null ? "" : inv.getCreatedAt().toLocalDate().format(DATE));
        tag(sb, 4, "InvoiceStatus", inv.getStatus().name());
        tag(sb, 4, "CustomerID", inv.getClient() == null ? "" : String.valueOf(inv.getClient().getId()));
        for (InvoiceLine line : inv.getLines()) {
            sb.append("        <Line>\n");
            tag(sb, 5, "ProductCode", line.getProduct() == null ? "" : line.getProduct().getSku());
            tag(sb, 5, "Quantity", plain(line.getQuantity()));
            tag(sb, 5, "UnitPrice", money(line.getUnitPrice()));
            tag(sb, 5, "TaxPercentage", money(percent(line.getTaxRate())));
            tag(sb, 5, "CreditAmount", money(line.getLineTotal()));
            sb.append("        </Line>\n");
        }
        sb.append("        <DocumentTotals>\n");
        tag(sb, 5, "TaxPayable", money(inv.getTaxAmount()));
        tag(sb, 5, "NetTotal", money(inv.getTotalBeforeTax()));
        tag(sb, 5, "GrossTotal", money(inv.getTotalAmount()));
        sb.append("        </DocumentTotals>\n");
        sb.append("      </Invoice>\n");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean withinPeriod(LocalDateTime when, LocalDateTime start, LocalDateTime end) {
        return when != null && !when.isBefore(start) && !when.isAfter(end);
    }

    private void tag(StringBuilder sb, int indent, String name, String rawValue) {
        sb.append("  ".repeat(indent))
          .append('<').append(name).append('>')
          .append(escape(rawValue))
          .append("</").append(name).append(">\n");
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(ch);
            }
        }
        return out.toString();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal scale(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal v) {
        return scale(v).toPlainString();
    }

    private String plain(BigDecimal v) {
        return nz(v).stripTrailingZeros().toPlainString();
    }

    /** Converte a taxa armazenada (0.16) em percentagem (16). */
    private BigDecimal percent(BigDecimal rate) {
        return nz(rate).multiply(BigDecimal.valueOf(100));
    }
}

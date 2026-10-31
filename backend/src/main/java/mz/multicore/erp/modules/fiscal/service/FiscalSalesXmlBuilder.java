package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.company.model.Company;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Component
public class FiscalSalesXmlBuilder {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public String buildXml(Company company, LocalDate from, LocalDate to,
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

    private void tag(StringBuilder sb, int indent, String name, String rawValue) {
        sb.append("  ".repeat(indent))
          .append('<').append(name).append('>')
          .append(escape(rawValue))
          .append("</").append(name).append(">\n");
    }

    public static String escape(String value) {
        if (value == null || value.isEmpty()) return "";
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

    public BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal scale(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal v) {
        return scale(v).toPlainString();
    }

    private String plain(BigDecimal v) {
        return nz(v).stripTrailingZeros().toPlainString();
    }

    public BigDecimal percent(BigDecimal rate) {
        return nz(rate).multiply(BigDecimal.valueOf(100));
    }
}

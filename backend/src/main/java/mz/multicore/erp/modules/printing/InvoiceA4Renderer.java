package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.company.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Compõe as vias A4 da factura sem conhecer persistência ou tenant. */
@Component
public class InvoiceA4Renderer {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LineRowMapper lineRowMapper;
    private final CompanyBankDetails companyBankDetails;

    @Autowired
    public InvoiceA4Renderer(LineRowMapper lineRowMapper, CompanyBankDetails companyBankDetails) {
        this.lineRowMapper = lineRowMapper;
        this.companyBankDetails = companyBankDetails;
    }

    public InvoiceA4Renderer(LineRowMapper lineRowMapper) {
        this(lineRowMapper, null);
    }

    byte[] render(Invoice invoice, List<String> copies) {
        List<String> bankDetails = companyBankDetails == null || invoice.getCompany() == null
                ? List.of() : companyBankDetails.forCompany(invoice.getCompany().getId());
        List<LineItemsTableRenderer.Row> rows = toRows(invoice.getLines());
        return PdfDocumentBuilder.buildA4(new InvoicePageFooter(invoice.getCompany(), copies), document -> {
            for (int index = 0; index < copies.size(); index++) {
                if (index > 0) document.newPage();
                renderCopy(document, invoice, copies.get(index), rows, bankDetails);
            }
        });
    }

    private void renderCopy(com.lowagie.text.Document document, Invoice invoice, String copy,
                            List<LineItemsTableRenderer.Row> rows, List<String> bankDetails) {
        document.add(header(invoice.getCompany(), invoice, copy));
        document.add(clientAndMetadata(invoice));
        document.add(LineItemsTableRenderer.buildModelTable(rows));
        document.add(TotalsBlockRenderer.buildModelTotals(
                invoice.getTotalBeforeTax(), totalDiscount(invoice), invoice.getTaxAmount(),
                invoice.getTotalAmount(), TotalsBlockRenderer.calculateBreakdowns(rows)));
        document.add(paymentAndNotes(invoice, bankDetails));
        document.add(legalDisclaimer());
    }

    private PdfPTable header(Company company, Invoice invoice, String copy) {
        PdfPTable table = twoColumns();
        table.setSpacingAfter(10f);
        PdfPCell companyCell = borderless();
        addLogo(companyCell, company);
        String name = company != null && company.getName() != null ? company.getName().toUpperCase() : "EMPRESA";
        Paragraph nameParagraph = new Paragraph(name,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, PdfTheme.BRAND));
        nameParagraph.setSpacingAfter(2f);
        companyCell.addElement(nameParagraph);
        addCompanyDetails(companyCell, company);
        table.addCell(companyCell);

        PdfPCell documentCell = borderless();
        addRight(documentCell, "FATURA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, PdfTheme.BRAND));
        addRight(documentCell, "N.º " + valueOrDash(invoice.getInvoiceNumber()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, PdfTheme.BRAND));
        addRight(documentCell, copy, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PdfTheme.MUTED));
        table.addCell(documentCell);
        return table;
    }

    private PdfPTable clientAndMetadata(Invoice invoice) {
        PdfPTable table = twoColumns();
        table.setSpacingAfter(12f);
        Client client = invoice.getClient();
        PdfPCell buyer = borderless();
        buyer.addElement(new Paragraph("FATURADO A", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED)));
        String buyerName = invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank()
                ? invoice.getCustomerName().trim() : client != null ? client.getName() : "Consumidor Final";
        buyer.addElement(new Paragraph(buyerName, PdfTheme.boldFont()));
        if (client != null && useful(client.getAddress())) buyer.addElement(new Paragraph(client.getAddress().trim(), PdfTheme.bodyFont()));
        String taxId = client != null && useful(client.getTaxId()) ? client.getTaxId().trim() : "Consumidor Final";
        buyer.addElement(new Paragraph("NUIT: " + taxId, PdfTheme.bodyFont()));
        table.addCell(buyer);

        PdfPTable grid = new PdfPTable(new float[]{50f, 50f});
        grid.setWidthPercentage(100);
        String issue = invoice.getCreatedAt() == null ? "—" : invoice.getCreatedAt().format(DATE_FMT);
        LocalDate due = invoice.effectiveDueDate();
        int days = client == null ? 0 : client.effectivePaymentTermsDays();
        meta(grid, "DATA DE EMISSÃO", issue);
        meta(grid, "VENCIMENTO", due == null ? issue : due.format(DATE_FMT));
        meta(grid, "COND. PAGAMENTO", days == 0 ? "Pronto Pagamento" : days + " dias");
        meta(grid, "MOEDA", "Meticais (MT)");
        PdfPCell metadata = borderless();
        metadata.addElement(grid);
        table.addCell(metadata);
        return table;
    }

    private PdfPTable paymentAndNotes(Invoice invoice, List<String> bankDetails) {
        PdfPTable table = twoColumns();
        table.setSpacingBefore(6f);
        table.setSpacingAfter(8f);
        PdfPCell payment = borderless();
        payment.setPaddingRight(10f);
        payment.addElement(title("DADOS PARA PAGAMENTO"));
        bankDetails.forEach(detail -> payment.addElement(new Paragraph(detail, PdfTheme.bodyFont())));
        payment.addElement(new Paragraph("Referência: " + valueOrDash(invoice.getInvoiceNumber()), PdfTheme.bodyFont()));
        table.addCell(payment);

        PdfPCell notes = borderless();
        notes.setPaddingLeft(10f);
        notes.addElement(title("OBSERVAÇÕES"));
        StringBuilder text = new StringBuilder("Prazo de entrega e garantia conforme condições comerciais contratadas.");
        append(text, "Motivo de Anulação: ", invoice.getCancellationReason());
        append(text, "Emitido em regime de contingência: ", invoice.getContingencyReference());
        if (invoice.getStatus() != null) text.append("\nEstado: ").append(DocumentLabels.status(invoice.getStatus()));
        notes.addElement(new Paragraph(text.toString(), PdfTheme.smallFont()));
        table.addCell(notes);
        return table;
    }

    private BigDecimal totalDiscount(Invoice invoice) {
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine line : invoice.getLines()) {
            if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0
                    && line.getUnitPrice() != null && line.getQuantity() != null) {
                BigDecimal rate = line.getDiscountPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                total = total.add(line.getUnitPrice().multiply(line.getQuantity()).multiply(rate));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private List<LineItemsTableRenderer.Row> toRows(List<InvoiceLine> lines) {
        return lines.stream().map(line -> lineRowMapper.map(line.getProduct(), line.getBatchNumber(),
                line.getQuantity(), line.getUnitPrice(), line.getTaxRate(), line.getDiscountPercentage(),
                line.getLineTotal())).toList();
    }

    private void addLogo(PdfPCell cell, Company company) {
        if (company == null || company.getLogo() == null || company.getLogo().length == 0) return;
        try {
            Image logo = Image.getInstance(company.getLogo());
            logo.scaleToFit(140f, 45f);
            logo.setSpacingAfter(4f);
            cell.addElement(logo);
        } catch (Exception ignored) { }
    }

    private void addCompanyDetails(PdfPCell cell, Company company) {
        if (company == null) return;
        if (useful(company.getAddress())) cell.addElement(new Paragraph(company.getAddress().trim(), PdfTheme.bodyFont()));
        String taxAndPhone = "NUIT: " + valueOrDash(company.getTaxId());
        if (useful(company.getPhone())) taxAndPhone += " · Tel.: " + company.getPhone().trim();
        cell.addElement(new Paragraph(taxAndPhone, PdfTheme.bodyFont()));
        if (useful(company.getEmail())) cell.addElement(new Paragraph("E-mail: " + company.getEmail().trim(), PdfTheme.bodyFont()));
    }

    private Paragraph legalDisclaimer() {
        Paragraph paragraph = new Paragraph("Os bens/serviços foram colocados à disposição do adquirente à data da emissão desta fatura. "
                + "Em caso de atraso no pagamento poderão ser cobrados juros de mora à taxa legal em vigor. "
                + "Obrigado pela sua preferência.", FontFactory.getFont(FontFactory.HELVETICA, 7.5f, PdfTheme.MUTED));
        paragraph.setSpacingBefore(6f);
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private PdfPTable twoColumns() { PdfPTable table = new PdfPTable(new float[]{55f, 45f}); table.setWidthPercentage(100); return table; }
    private PdfPCell borderless() { PdfPCell cell = new PdfPCell(); cell.setBorder(PdfPCell.NO_BORDER); return cell; }
    private Paragraph title(String text) { Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED)); p.setSpacingAfter(3f); return p; }
    private void addRight(PdfPCell cell, String text, com.lowagie.text.Font font) { Paragraph p = new Paragraph(text, font); p.setAlignment(Element.ALIGN_RIGHT); cell.addElement(p); }
    private void meta(PdfPTable grid, String label, String value) { PdfPCell cell = borderless(); cell.setPaddingBottom(6f); cell.addElement(new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, PdfTheme.MUTED))); cell.addElement(new Paragraph(value, PdfTheme.bodyFont())); grid.addCell(cell); }
    private void append(StringBuilder text, String label, String value) { if (useful(value)) text.append('\n').append(label).append(value.trim()); }
    private boolean useful(String value) { return value != null && !value.isBlank() && !"-".equals(value) && !"—".equals(value); }
    private String valueOrDash(String value) { return value == null ? "—" : value; }
}

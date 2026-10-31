package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Compõe a proposta comercial A4 sem conhecer persistência ou tenant. */
@Component
public class QuotationA4Renderer {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public QuotationA4Renderer(LineRowMapper lineRowMapper, DocumentConfigService documentConfigService) {
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    byte[] render(Quotation quotation) {
        List<LineItemsTableRenderer.Row> rows = rows(quotation.getLines());
        return PdfDocumentBuilder.buildA4(document -> {
            document.add(header(quotation.getCompany(), quotation));
            document.add(clientAndMetadata(quotation));
            document.add(validity(quotation));
            document.add(LineItemsTableRenderer.buildModelTable(rows));
            document.add(TotalsBlockRenderer.buildModelTotals(
                    quotation.getTotalBeforeTax(), totalDiscount(quotation.getLines()),
                    quotation.getTaxAmount(), quotation.getTotalAmount(),
                    TotalsBlockRenderer.calculateBreakdowns(rows)));
            document.add(PdfDocumentBuilder.spacer(8f));
            PdfPTable terms = CommercialTermsRenderer.build(
                    quotation.getPaymentTerms(), quotation.getDeliveryTerms(), null, quotation.getNotes());
            if (terms != null) document.add(terms);
            addConfiguredFooter(document, quotation);
            document.add(PdfDocumentBuilder.spacer(14f));
            document.add(SignatureBlockRenderer.build(
                    "Pela empresa", "Aceite pelo cliente (nome, assinatura e data)"));
            document.add(PdfDocumentBuilder.spacer(8f));
            document.add(disclaimer());
        });
    }

    private PdfPTable header(Company company, Quotation quotation) {
        PdfPTable table = twoColumns();
        table.setSpacingAfter(10f);
        PdfPCell companyCell = borderless();
        addLogo(companyCell, company);
        String name = company != null && company.getName() != null ? company.getName().toUpperCase() : "EMPRESA";
        Paragraph companyName = new Paragraph(name,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, PdfTheme.BRAND));
        companyName.setSpacingAfter(2f);
        companyCell.addElement(companyName);
        addCompanyDetails(companyCell, company);
        table.addCell(companyCell);

        PdfPCell documentCell = borderless();
        addRight(documentCell, "COTAÇÃO",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, PdfTheme.BRAND));
        addRight(documentCell, "N.º " + valueOrDash(quotation.getQuotationNumber()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, PdfTheme.BRAND));
        String status = quotation.getStatus() == null ? "—" : quotation.getStatus().getLabel();
        addRight(documentCell, "Estado: " + status,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PdfTheme.MUTED));
        table.addCell(documentCell);
        return table;
    }

    private PdfPTable clientAndMetadata(Quotation quotation) {
        PdfPTable table = twoColumns();
        table.setSpacingAfter(12f);
        Client client = quotation.getClient();
        PdfPCell buyer = borderless();
        buyer.addElement(label("PROPOSTA PARA"));
        String buyerName = useful(quotation.getWalkInName()) ? quotation.getWalkInName().trim()
                : client != null ? client.getName() : "Consumidor Final";
        buyer.addElement(new Paragraph(buyerName, PdfTheme.boldFont()));
        if (client != null && usefulAddress(client.getAddress())) {
            buyer.addElement(new Paragraph(client.getAddress().trim(), PdfTheme.bodyFont()));
        }
        String taxId = client != null && useful(client.getTaxId()) ? client.getTaxId().trim() : "Consumidor Final";
        buyer.addElement(new Paragraph("NUIT: " + taxId, PdfTheme.bodyFont()));
        table.addCell(buyer);

        PdfPTable grid = new PdfPTable(new float[]{50f, 50f});
        grid.setWidthPercentage(100);
        String issue = quotation.getQuotationDate() == null ? "—" : quotation.getQuotationDate().format(DATE_FMT);
        String valid = quotation.getValidUntil() == null ? "—" : quotation.getValidUntil().format(DATE_FMT);
        String payment = useful(quotation.getPaymentTerms()) ? quotation.getPaymentTerms().trim()
                : client != null && client.effectivePaymentTermsDays() > 0
                ? client.effectivePaymentTermsDays() + " dias" : "Pronto Pagamento";
        meta(grid, "DATA DA PROPOSTA", issue);
        meta(grid, "VALIDADE", valid);
        meta(grid, "COND. PAGAMENTO", payment);
        meta(grid, "REFERÊNCIA", valueOrDash(quotation.getQuotationNumber()));
        PdfPCell metadata = borderless();
        metadata.addElement(grid);
        table.addCell(metadata);
        return table;
    }

    private PdfPTable validity(Quotation quotation) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        cell.setPadding(8f);
        cell.addElement(new Paragraph("Proposta válida até " + quotation.getValidUntil().format(DATE_FMT),
                PdfTheme.subtitleFont()));
        String message = quotation.isExpired(LocalDate.now())
                ? "ATENÇÃO: esta cotação já caducou. Os preços apresentados carecem de nova confirmação."
                : "Os preços e condições abaixo são garantidos até à data indicada.";
        cell.addElement(new Paragraph(message,
                quotation.isExpired(LocalDate.now()) ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        table.addCell(cell);
        return table;
    }

    private void addConfiguredFooter(com.lowagie.text.Document document, Quotation quotation) {
        var columns = documentConfigService.getColumns(quotation.getCompany().getId(), DocumentType.COMMERCIAL);
        if (columns != null && useful(columns.footer())) {
            Paragraph footer = new Paragraph(columns.footer().trim(), PdfTheme.smallFont());
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(6f);
            document.add(footer);
        }
    }

    private BigDecimal totalDiscount(List<QuotationLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (QuotationLine line : lines) {
            if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0
                    && line.getUnitPrice() != null && line.getQuantity() != null) {
                BigDecimal rate = line.getDiscountPercentage()
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                total = total.add(line.getUnitPrice().multiply(line.getQuantity()).multiply(rate));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private List<LineItemsTableRenderer.Row> rows(List<QuotationLine> lines) {
        return lines.stream().map(line -> lineRowMapper.map(line.getProduct(), null,
                line.getQuantity(), line.getUnitPrice(), line.getTaxRate(),
                line.getDiscountPercentage(), line.getLineTotal())).toList();
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

    private Paragraph disclaimer() {
        Paragraph paragraph = new Paragraph("Este documento é uma proposta comercial. Não constitui documento fiscal nem substitui a "
                + "factura, que será emitida após confirmação da encomenda.", PdfTheme.smallFont());
        paragraph.setAlignment(Element.ALIGN_LEFT);
        return paragraph;
    }

    private PdfPTable twoColumns() { PdfPTable table = new PdfPTable(new float[]{55f, 45f}); table.setWidthPercentage(100); return table; }
    private PdfPCell borderless() { PdfPCell cell = new PdfPCell(); cell.setBorder(PdfPCell.NO_BORDER); return cell; }
    private Paragraph label(String text) { Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED)); p.setSpacingAfter(2f); return p; }
    private void addRight(PdfPCell cell, String text, com.lowagie.text.Font font) { Paragraph p = new Paragraph(text, font); p.setAlignment(Element.ALIGN_RIGHT); cell.addElement(p); }
    private void meta(PdfPTable grid, String title, String value) { PdfPCell cell = borderless(); cell.setPaddingBottom(6f); cell.addElement(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, PdfTheme.MUTED))); cell.addElement(new Paragraph(value, PdfTheme.bodyFont())); grid.addCell(cell); }
    private boolean useful(String value) { return value != null && !value.isBlank(); }
    private boolean usefulAddress(String value) { return useful(value) && !"-".equals(value) && !"—".equals(value) && !"n/a".equalsIgnoreCase(value); }
    private String valueOrDash(String value) { return value == null ? "—" : value; }
}

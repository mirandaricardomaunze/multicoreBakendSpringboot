package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderStatusLabel;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Compõe a encomenda comercial A4 sem conhecer persistência ou tenant. */
@Component
public class OrderA4Renderer {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public OrderA4Renderer(LineRowMapper lineRowMapper, DocumentConfigService documentConfigService) {
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    byte[] render(Order order) {
        List<LineItemsTableRenderer.Row> rows = rows(order.getLines());
        return PdfDocumentBuilder.buildA4(document -> {
            document.add(header(order.getCompany(), order));
            document.add(clientAndMetadata(order));
            if (useful(order.getQuotationNumber())) {
                Paragraph origin = new Paragraph("Origem: Cotação " + order.getQuotationNumber(), PdfTheme.boldFont());
                origin.setSpacingAfter(6f);
                document.add(origin);
            }
            document.add(LineItemsTableRenderer.buildModelTable(rows));
            addGrossWeight(document, order);
            document.add(TotalsBlockRenderer.buildModelTotals(order.getTotalBeforeTax(),
                    totalDiscount(order.getLines()), order.getTaxAmount(), order.getTotalAmount(),
                    TotalsBlockRenderer.calculateBreakdowns(rows)));
            document.add(PdfDocumentBuilder.spacer(8f));
            PdfPTable terms = CommercialTermsRenderer.build(order.getPaymentTerms(),
                    order.getDeliveryTerms(), order.getExpectedDeliveryDate(), null);
            if (terms != null) document.add(terms);
            PdfPTable footer = footer(order);
            if (footer != null) document.add(footer);
            document.add(PdfDocumentBuilder.spacer(14f));
            document.add(SignatureBlockRenderer.build("Pela empresa", "Confirmação do cliente"));
        });
    }

    private PdfPTable header(Company company, Order order) {
        PdfPTable table = twoColumns();
        table.setSpacingAfter(10f);
        PdfPCell companyCell = borderless();
        addLogo(companyCell, company);
        String name = company != null && company.getName() != null ? company.getName().toUpperCase() : "EMPRESA";
        Paragraph companyName = new Paragraph(name, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, PdfTheme.BRAND));
        companyName.setSpacingAfter(2f);
        companyCell.addElement(companyName);
        addCompanyDetails(companyCell, company);
        table.addCell(companyCell);
        PdfPCell documentCell = borderless();
        addRight(documentCell, "ENCOMENDA", 22f, PdfTheme.BRAND);
        addRight(documentCell, "N.º " + valueOrDash(order.getOrderNumber()), 11f, PdfTheme.BRAND);
        addRight(documentCell, "Estado: " + OrderStatusLabel.of(order.getStatus()), 8.5f, PdfTheme.MUTED);
        table.addCell(documentCell);
        return table;
    }

    private PdfPTable clientAndMetadata(Order order) {
        PdfPTable table = twoColumns();
        table.setSpacingAfter(12f);
        Client client = order.getClient();
        PdfPCell buyer = borderless();
        buyer.addElement(label("ENCOMENDADO POR"));
        String buyerName = useful(order.getWalkInName()) ? order.getWalkInName().trim()
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
        String issue = order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "—";
        String delivery = order.getExpectedDeliveryDate() != null ? order.getExpectedDeliveryDate().format(DATE_FMT) : "—";
        String payment = useful(order.getPaymentTerms()) ? order.getPaymentTerms().trim()
                : client != null && client.effectivePaymentTermsDays() > 0
                ? client.effectivePaymentTermsDays() + " dias" : "Pronto Pagamento";
        meta(grid, "DATA DE EMISSÃO", issue);
        meta(grid, "PREVISÃO ENTREGA", delivery);
        meta(grid, "COND. PAGAMENTO", payment);
        meta(grid, "REFERÊNCIA", valueOrDash(order.getOrderNumber()));
        PdfPCell metadata = borderless();
        metadata.addElement(grid);
        table.addCell(metadata);
        return table;
    }

    private void addGrossWeight(com.lowagie.text.Document document, Order order) {
        BigDecimal weight = order.getLines().stream()
                .map(line -> line.getProduct().getGrossUnitWeightKg() == null ? BigDecimal.ZERO
                        : line.getQuantity().multiply(line.getProduct().getGrossUnitWeightKg()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weight.signum() <= 0) return;
        Paragraph paragraph = new Paragraph("Peso bruto total: "
                + weight.setScale(3, RoundingMode.HALF_UP) + " kg", PdfTheme.boldFont());
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        paragraph.setSpacingAfter(4f);
        document.add(paragraph);
    }

    private PdfPTable footer(Order order) {
        var columns = documentConfigService.getColumns(order.getCompany().getId(), DocumentType.COMMERCIAL);
        String companyFooter = columns != null ? columns.footer() : null;
        boolean hasNotes = useful(order.getNotes());
        boolean hasFooter = useful(companyFooter);
        if (!hasNotes && !hasFooter) return null;
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(4f);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setPadding(6f);
        if (hasNotes) {
            Paragraph title = new Paragraph("Observações / Instruções:", PdfTheme.boldFont());
            title.setSpacingAfter(2f);
            cell.addElement(title);
            Paragraph note = new Paragraph(order.getNotes().trim(), PdfTheme.bodyFont());
            note.setSpacingAfter(hasFooter ? 4f : 0f);
            cell.addElement(note);
        }
        if (hasFooter) {
            Paragraph configured = new Paragraph(companyFooter.trim(), PdfTheme.smallFont());
            configured.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(configured);
        }
        table.addCell(cell);
        return table;
    }

    private BigDecimal totalDiscount(List<OrderLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0
                    && line.getUnitPrice() != null && line.getQuantity() != null) {
                BigDecimal rate = line.getDiscountPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                total = total.add(line.getUnitPrice().multiply(line.getQuantity()).multiply(rate));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private List<LineItemsTableRenderer.Row> rows(List<OrderLine> lines) {
        return lines.stream().map(line -> lineRowMapper.map(line.getProduct(), line.getBatchNumber(),
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

    private PdfPTable twoColumns() { PdfPTable table = new PdfPTable(new float[]{55f, 45f}); table.setWidthPercentage(100); return table; }
    private PdfPCell borderless() { PdfPCell cell = new PdfPCell(); cell.setBorder(PdfPCell.NO_BORDER); return cell; }
    private Paragraph label(String text) { Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED)); p.setSpacingAfter(2f); return p; }
    private void addRight(PdfPCell cell, String text, float size, java.awt.Color color) { Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, size, color)); p.setAlignment(Element.ALIGN_RIGHT); cell.addElement(p); }
    private void meta(PdfPTable grid, String title, String value) { PdfPCell cell = borderless(); cell.setPaddingBottom(6f); cell.addElement(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, PdfTheme.MUTED))); cell.addElement(new Paragraph(value, PdfTheme.bodyFont())); grid.addCell(cell); }
    private boolean useful(String value) { return value != null && !value.isBlank(); }
    private boolean usefulAddress(String value) { return useful(value) && !"-".equals(value) && !"—".equals(value) && !"n/a".equalsIgnoreCase(value); }
    private String valueOrDash(String value) { return value == null ? "—" : value; }
}

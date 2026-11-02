package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DeliveryGuideDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public DeliveryGuideDocumentRenderer(LineRowMapper lineRowMapper, DocumentConfigService documentConfigService) {
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    public byte[] renderDocument(DeliveryGuide guide, List<String> copies) {
        List<LineItemsTableRenderer.Row> rows = toRows(guide.getLines());
        var raw = documentConfigService.getColumns(guide.getCompany().getId(), DocumentType.COMMERCIAL);
        var columns = new DocumentColumnsDTO(
                raw.barcode(), raw.reference(), raw.description(), raw.expiry(),
                raw.quantity(), raw.packages(), raw.boxes(), raw.boxPercentage(),
                true, raw.tax(), true, raw.footer()
        );

        return PdfDocumentBuilder.build(PageSize.A4, doc -> {
            for (int i = 0; i < copies.size(); i++) {
                if (i > 0) doc.newPage();
                renderDeliveryGuideCopy(doc, guide, copies.get(i), rows, columns);
            }
        });
    }

    private void renderDeliveryGuideCopy(Document doc, DeliveryGuide guide, String copyLabel,
                                         List<LineItemsTableRenderer.Row> rows, DocumentColumnsDTO columns) {
        doc.add(CompanyHeaderRenderer.build(guide.getCompany(), "Guia de Remessa", guide.getGuideNumber(), copyLabel, null));
        doc.add(buildDeliveryBlock(guide));
        doc.add(LineItemsTableRenderer.buildCompact(rows, columns));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        if (guide.getLines() != null) {
            for (DeliveryGuideLine l : guide.getLines()) {
                BigDecimal price = l.getUnitPrice() != null ? l.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : BigDecimal.ZERO;
                var amounts = LineCalculator.compute(price, qty, l.getDiscountPercentage(), l.getTaxRate());
                subtotal = subtotal.add(amounts.net());
                tax = tax.add(amounts.tax());
            }
        }
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        doc.add(TotalsBlockRenderer.build(subtotal.setScale(2, RoundingMode.HALF_UP), tax.setScale(2, RoundingMode.HALF_UP), total, "VALOR DECLARADO"));

        Paragraph packageSummary = buildPackageSummary(guide.getLines());
        if (packageSummary != null) doc.add(packageSummary);
        doc.add(PdfDocumentBuilder.spacer(8f));
        doc.add(buildTransportBlock(guide));
        doc.add(PdfDocumentBuilder.spacer(24f));
        doc.add(SignatureBlockRenderer.build("O Expedidor", "Recebi em conformidade (Destinatário)"));
    }

    private PdfPTable buildDeliveryBlock(DeliveryGuide guide) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell client = new PdfPCell();
        client.setBorder(PdfPCell.NO_BORDER);
        client.addElement(new Paragraph("Destinatário", PdfTheme.subtitleFont()));
        String name = (guide.getWalkInName() != null && !guide.getWalkInName().isBlank())
                ? guide.getWalkInName()
                : (guide.getClient() != null ? guide.getClient().getName() : "");
        client.addElement(new Paragraph(name, PdfTheme.bodyFont()));
        if (guide.getClient() != null && guide.getClient().getTaxId() != null) {
            client.addElement(new Paragraph("NUIT: " + guide.getClient().getTaxId(), PdfTheme.bodyFont()));
        }
        if (guide.getClient() != null && guide.getClient().getAddress() != null) {
            client.addElement(new Paragraph("Local de entrega: " + guide.getClient().getAddress(), PdfTheme.bodyFont()));
        }
        table.addCell(client);

        PdfPCell meta = new PdfPCell();
        meta.setBorder(PdfPCell.NO_BORDER);
        if (guide.getOrderNumber() != null) addRight(meta, "Encomenda de origem: " + guide.getOrderNumber());
        if (guide.getInvoiceNumber() != null) addRight(meta, "Fatura: " + guide.getInvoiceNumber());
        if (guide.getGuideDate() != null) addRight(meta, "Data: " + guide.getGuideDate().format(DATE_FMT));
        if (guide.getWarehouse() != null) addRight(meta, "Origem (armazém): " + guide.getWarehouse().getName());
        table.addCell(meta);
        return table;
    }

    private PdfPTable buildTransportBlock(DeliveryGuide guide) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Transporte", PdfTheme.subtitleFont()));
        String carrier = guide.getResponsible() != null ? guide.getResponsible() : "____________________________";
        String plate = guide.getVehicle() != null ? guide.getVehicle() : "________________________________";
        left.addElement(new Paragraph("Transportador: " + carrier, PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Matrícula: " + plate, PdfTheme.bodyFont()));
        table.addCell(left);

        BigDecimal totalWeight = BigDecimal.ZERO;
        int totalPackages = 0;
        if (guide.getLines() != null) {
            for (DeliveryGuideLine l : guide.getLines()) {
                Product p = l.getProduct();
                if (p != null) {
                    BigDecimal w = p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg();
                    if (w != null && l.getQuantity() != null) totalWeight = totalWeight.add(w.multiply(l.getQuantity()));
                    int factor = Math.max(1, p.getUnitsPerBox());
                    totalPackages += l.getQuantity() != null ? l.getQuantity().intValue() / factor : 0;
                }
            }
        }

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.addElement(new Paragraph("Carga & Expedição", PdfTheme.subtitleFont()));
        right.addElement(new Paragraph("Data/Hora de carga: ______________________", PdfTheme.bodyFont()));
        String weightStr = totalWeight.compareTo(BigDecimal.ZERO) > 0
                ? totalWeight.setScale(3, RoundingMode.HALF_UP) + " kg"
                : "___________ kg (Aferir na balança)";
        right.addElement(new Paragraph("Peso Bruto da Carga: " + weightStr, PdfTheme.bodyFont()));
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            right.addElement(new Paragraph("Viatura Recomendada: " + LogisticsLoadCalculator.recommendedVehicleType(totalWeight), PdfTheme.smallFont()));
        }
        right.addElement(new Paragraph("Nº de volumes: " + (totalPackages > 0 ? totalPackages + " vol." : "___________"), PdfTheme.bodyFont()));
        table.addCell(right);
        return table;
    }

    private void addRight(PdfPCell cell, String text) {
        Paragraph p = new Paragraph(text, PdfTheme.bodyFont());
        p.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(p);
    }

    private List<LineItemsTableRenderer.Row> toRows(List<DeliveryGuideLine> lines) {
        return lines.stream().map(l -> lineRowMapper.map(
                l.getProduct(), l.getBatchNumber(), l.getQuantity(),
                l.getUnitPrice(), l.getTaxRate(), l.getDiscountPercentage(), l.getLineTotal()
        )).toList();
    }

    private Paragraph buildPackageSummary(List<DeliveryGuideLine> lines) {
        if (lines == null || lines.isEmpty()) return null;
        return LoadSummaryRenderer.build(lines.stream().map(line -> {
            Product p = line.getProduct();
            BigDecimal weight = p != null ? (p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg()) : null;
            int unitsPerBox = p != null ? p.getUnitsPerBox() : 1;
            String name = p != null ? p.getName() : "Artigo";
            return new LoadSummaryRenderer.Item(name, line.getQuantity(), weight, unitsPerBox);
        }).toList());
    }
}

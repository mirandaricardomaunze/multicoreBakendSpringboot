package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Renderizador A4 paisagem das 4 vias da Guia de Remessa derivada de factura. */
@Component
public class GuideRemittanceDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;
    private final OrderRepository orderRepository;

    @Autowired
    public GuideRemittanceDocumentRenderer(LineRowMapper lineRowMapper,
                                           DocumentConfigService documentConfigService,
                                           OrderRepository orderRepository) {
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
        this.orderRepository = orderRepository;
    }

    public GuideRemittanceDocumentRenderer(LineRowMapper lineRowMapper,
                                           DocumentConfigService documentConfigService) {
        this(lineRowMapper, documentConfigService, null);
    }

    public byte[] render(Invoice invoice) {
        List<LineItemsTableRenderer.Row> rows = toRows(invoice.getLines());
        var rawColumns = documentConfigService.getColumns(invoice.getCompany().getId(), DocumentType.COMMERCIAL);
        var columns = new DocumentColumnsDTO(
                rawColumns.barcode(), rawColumns.reference(), rawColumns.description(),
                rawColumns.expiry(), rawColumns.quantity(), rawColumns.packages(),
                rawColumns.boxes(), rawColumns.boxPercentage(), true,
                rawColumns.tax(), true, rawColumns.footer()
        );

        return PdfDocumentBuilder.build(PageSize.A4, doc -> {
            List<String> copies = GuideRemittancePrintService.GUIDE_REMITTANCE_COPIES;
            for (int i = 0; i < copies.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderGuideCopy(doc, invoice, copies.get(i), rows, columns);
            }
        });
    }

    private void renderGuideCopy(Document doc, Invoice invoice, String copyLabel,
                                 List<LineItemsTableRenderer.Row> rows, DocumentColumnsDTO columns) {
        doc.add(CompanyHeaderRenderer.build(invoice.getCompany(), "Guia de Remessa",
                "GR-" + invoice.getInvoiceNumber(), copyLabel, null));
        doc.add(buildDeliveryBlock(invoice));
        doc.add(LineItemsTableRenderer.buildCompact(rows, columns));

        BigDecimal subtotal = invoice.getTotalBeforeTax() != null ? invoice.getTotalBeforeTax() : BigDecimal.ZERO;
        BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : subtotal.add(tax);
        doc.add(TotalsBlockRenderer.build(subtotal, tax, total, "VALOR DECLARADO"));

        Paragraph packageSummary = buildPackageSummary(invoice.getLines());
        if (packageSummary != null) {
            doc.add(packageSummary);
        }

        doc.add(buildStockNotice(invoice));
        doc.add(PdfDocumentBuilder.spacer(8f));
        doc.add(buildTransportBlock(invoice));
        doc.add(PdfDocumentBuilder.spacer(24f));
        doc.add(buildSignatureBlock());
    }

    private Paragraph buildPackageSummary(List<InvoiceLine> lines) {
        if (lines == null || lines.isEmpty()) return null;
        return LoadSummaryRenderer.build(lines.stream()
                .map(l -> {
                    Product p = l.getProduct();
                    BigDecimal weight = p != null
                            ? (p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg())
                            : null;
                    int unitsPerBox = p != null ? p.getUnitsPerBox() : 1;
                    String name = p != null ? p.getName() : "Artigo";
                    return new LoadSummaryRenderer.Item(name, l.getQuantity(), weight, unitsPerBox);
                })
                .toList());
    }

    private static Paragraph buildStockNotice(Invoice invoice) {
        Font noticeFont = new Font(Font.HELVETICA, 8f, Font.ITALIC, new Color(71, 85, 105));
        Paragraph p = new Paragraph("Nota: Documento exclusivamente de transporte e trânsito com valor declarado para fiscalização — "
                + "Não movimenta stock (saída de mercadoria efectuada na Fatura " + invoice.getInvoiceNumber() + ").", noticeFont);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(4f);
        return p;
    }

    private PdfPTable buildDeliveryBlock(Invoice invoice) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell client = new PdfPCell();
        client.setBorder(PdfPCell.NO_BORDER);
        client.addElement(new Paragraph("Destinatário", PdfTheme.subtitleFont()));
        if (invoice.getClient() != null) {
            client.addElement(new Paragraph(invoice.getClient().getName(), PdfTheme.bodyFont()));
            if (invoice.getClient().getTaxId() != null) {
                client.addElement(new Paragraph("NUIT: " + invoice.getClient().getTaxId(), PdfTheme.bodyFont()));
            }
            if (invoice.getClient().getAddress() != null) {
                client.addElement(new Paragraph("Local de entrega: " + invoice.getClient().getAddress(), PdfTheme.bodyFont()));
            }
        }
        table.addCell(client);

        PdfPCell meta = new PdfPCell();
        meta.setBorder(PdfPCell.NO_BORDER);
        addRight(meta, "Fatura de origem: " + invoice.getInvoiceNumber());
        if (orderRepository != null) {
            orderRepository.findFirstByCompanyIdAndInvoiceId(invoice.getCompany().getId(), invoice.getId())
                    .ifPresent(o -> addRight(meta, "Encomenda: " + o.getOrderNumber()));
        }
        if (invoice.getCreatedAt() != null) {
            addRight(meta, "Data: " + invoice.getCreatedAt().format(DATE_FMT));
        }
        if (invoice.getWarehouse() != null) {
            addRight(meta, "Origem (armazém): " + invoice.getWarehouse().getName());
        }
        table.addCell(meta);
        return table;
    }

    private PdfPTable buildTransportBlock(Invoice invoice) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Transporte", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph("Transportador: ____________________________", PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Matrícula: ________________________________", PdfTheme.bodyFont()));
        table.addCell(left);

        BigDecimal totalWeight = BigDecimal.ZERO;
        int totalPackages = 0;
        if (invoice.getLines() != null) {
            for (InvoiceLine l : invoice.getLines()) {
                Product p = l.getProduct();
                if (p != null) {
                    BigDecimal w = p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg();
                    if (w != null && l.getQuantity() != null) {
                        totalWeight = totalWeight.add(w.multiply(l.getQuantity()));
                    }
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

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("O Expedidor"));
        table.addCell(signatureCell("Recebi em conformidade (Destinatário)"));
        return table;
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);
        cell.addElement(new Paragraph("____________________________", PdfTheme.bodyFont()));
        cell.addElement(new Paragraph(label, PdfTheme.smallFont()));
        return cell;
    }

    private void addRight(PdfPCell cell, String text) {
        Paragraph p = new Paragraph(text, PdfTheme.bodyFont());
        p.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(p);
    }

    private List<LineItemsTableRenderer.Row> toRows(List<InvoiceLine> lines) {
        return lines.stream().map(l -> lineRowMapper.map(
                l.getProduct(), l.getBatchNumber(), l.getQuantity(),
                l.getUnitPrice(), l.getTaxRate(), l.getDiscountPercentage(), l.getLineTotal()
        )).toList();
    }
}

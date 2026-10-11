package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera o PDF da Guia de Remessa ao cliente a partir do documento persistido {@link DeliveryGuide}.
 * Reutiliza o cabeçalho da empresa e o renderizador de linhas partilhado (mesmas colunas dos
 * restantes documentos), acrescentando bloco de destinatário/entrega, transporte e assinaturas.
 */
@Service
public class DeliveryGuidePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DeliveryGuideRepository guideRepository;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public DeliveryGuidePrintService(DeliveryGuideRepository guideRepository, LineRowMapper lineRowMapper,
                                     DocumentConfigService documentConfigService) {
        this.guideRepository = guideRepository;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    public static final List<String> DELIVERY_GUIDE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Destinatário / Cliente)",
            "2.ª VIA — DUPLICADO (Trânsito e Fiscalização / Viatura)",
            "3.ª VIA — TRIPLICADO (Expedição / Arquivo de Origem)",
            "4.ª VIA — QUADRUPLICADO (Comprovativo de Recepção e Devolução)"
    );

    @Transactional(readOnly = true)
    public byte[] render(Long guideId) {
        DeliveryGuide guide = guideRepository
                .findByIdWithLinesAndCompanyId(guideId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Guia de remessa não encontrada."));
        CurrentUserContext.requireCompany(guide.getCompany().getId());

        List<LineItemsTableRenderer.Row> rows = toRows(guide.getLines());
        var columns = documentConfigService.getColumns(guide.getCompany().getId(), DocumentType.COMMERCIAL);

        return PdfDocumentBuilder.buildA4(doc -> {
            for (int i = 0; i < DELIVERY_GUIDE_COPIES.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderDeliveryGuideCopy(doc, guide, DELIVERY_GUIDE_COPIES.get(i), rows, columns);
            }
        });
    }

    private void renderDeliveryGuideCopy(com.lowagie.text.Document doc, DeliveryGuide guide, String copyLabel,
                                         List<LineItemsTableRenderer.Row> rows,
                                         mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO columns) {
        doc.add(CompanyHeaderRenderer.build(
                guide.getCompany(),
                "Guia de Remessa",
                guide.getGuideNumber()
        ));
        doc.add(buildCopyBadge(copyLabel));
        doc.add(buildDeliveryBlock(guide));
        doc.add(LineItemsTableRenderer.build(rows, columns));

        // Totais e Valor da Mercadoria
        java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal tax = java.math.BigDecimal.ZERO;
        if (guide.getLines() != null) {
            for (DeliveryGuideLine l : guide.getLines()) {
                java.math.BigDecimal price = l.getUnitPrice() != null ? l.getUnitPrice() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal lineNet = price.multiply(qty);
                subtotal = subtotal.add(lineNet);
                if (l.getTaxRate() != null) {
                    tax = tax.add(lineNet.multiply(l.getTaxRate()));
                }
            }
        }
        java.math.BigDecimal total = guide.getTotalAmount() != null && guide.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                ? guide.getTotalAmount()
                : subtotal.add(tax).setScale(2, java.math.RoundingMode.HALF_UP);
        doc.add(TotalsBlockRenderer.build(subtotal.setScale(2, java.math.RoundingMode.HALF_UP),
                tax.setScale(2, java.math.RoundingMode.HALF_UP), total));

        Paragraph packageSummary = buildPackageSummary(guide.getLines());
        if (packageSummary != null) {
            doc.add(packageSummary);
        }
        doc.add(PdfDocumentBuilder.spacer(8f));
        doc.add(buildTransportBlock(guide));
        doc.add(PdfDocumentBuilder.spacer(24f));
        doc.add(SignatureBlockRenderer.build(
                "O Expedidor", "Recebi em conformidade (Destinatário)"));
    }

    private static PdfPTable buildCopyBadge(String copyText) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        table.setSpacingAfter(8f);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new java.awt.Color(241, 245, 249)); // Slate-100
        cell.setBorderColor(new java.awt.Color(203, 213, 225));     // Slate-300
        cell.setBorderWidth(1f);
        cell.setPadding(5f);

        com.lowagie.text.Font badgeFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 8.5f, com.lowagie.text.Font.BOLD, new java.awt.Color(30, 41, 59));
        Paragraph p = new Paragraph(copyText, badgeFont);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        table.addCell(cell);
        return table;
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
        if (guide.getOrderNumber() != null) {
            addRight(meta, "Encomenda de origem: " + guide.getOrderNumber());
        }
        if (guide.getGuideDate() != null) {
            addRight(meta, "Data: " + guide.getGuideDate().format(DATE_FMT));
        }
        if (guide.getWarehouse() != null) {
            addRight(meta, "Origem (armazém): " + guide.getWarehouse().getName());
        }
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

        java.math.BigDecimal totalWeight = java.math.BigDecimal.ZERO;
        int totalPackages = 0;
        if (guide.getLines() != null) {
            for (DeliveryGuideLine l : guide.getLines()) {
                Product p = l.getProduct();
                if (p != null) {
                    java.math.BigDecimal w = p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg();
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
        String weightStr = totalWeight.compareTo(java.math.BigDecimal.ZERO) > 0
                ? totalWeight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg"
                : "___________ kg (Aferir na balança)";
        right.addElement(new Paragraph("Peso Bruto da Carga: " + weightStr, PdfTheme.bodyFont()));
        if (totalWeight.compareTo(java.math.BigDecimal.ZERO) > 0) {
            right.addElement(new Paragraph("Viatura Recomendada: " + mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.recommendedVehicleType(totalWeight), PdfTheme.smallFont()));
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
                l.getProduct(),
                l.getBatchNumber(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }

    /**
     * Resumo de carga. A conta vive no {@link LoadSummaryRenderer}, partilhado com a guia de
     * transferência entre armazéns — os dois documentos movimentam mercadoria e respondem à
     * mesma pergunta ("o que é que isto pesa?"), pelo que não podem ter duas contas diferentes.
     */
    private Paragraph buildPackageSummary(List<DeliveryGuideLine> lines) {
        if (lines == null || lines.isEmpty()) return null;
        return LoadSummaryRenderer.build(lines.stream()
                .map(line -> {
                    Product p = line.getProduct();
                    java.math.BigDecimal weight = p != null
                            ? (p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg())
                            : null;
                    int unitsPerBox = p != null ? p.getUnitsPerBox() : 1;
                    String name = p != null ? p.getName() : "Artigo";
                    return new LoadSummaryRenderer.Item(name, line.getQuantity(), weight, unitsPerBox);
                })
                .toList());
    }
}

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

    @Transactional(readOnly = true)
    public byte[] render(Long guideId) {
        DeliveryGuide guide = guideRepository
                .findByIdWithLinesAndCompanyId(guideId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Guia de remessa não encontrada."));
        CurrentUserContext.requireCompany(guide.getCompany().getId());

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    guide.getCompany(),
                    "Guia de Remessa",
                    guide.getGuideNumber()
            ));
            doc.add(buildDeliveryBlock(guide));
            doc.add(LineItemsTableRenderer.build(toRows(guide.getLines()),
                    documentConfigService.getColumns(guide.getCompany().getId(), DocumentType.COMMERCIAL)));
            doc.add(buildPackageSummary(guide.getLines()));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildTransportBlock(guide));
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(buildSignatureBlock());
        });
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

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.addElement(new Paragraph(" ", PdfTheme.subtitleFont()));
        right.addElement(new Paragraph("Data/Hora de carga: ______________________", PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Nº de volumes: ___________________________", PdfTheme.bodyFont()));
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

    private Paragraph buildPackageSummary(List<DeliveryGuideLine> lines) {
        var shares = mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.calculate(lines.stream()
                .map(line -> new mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.Input(
                        line.getQuantity(), line.getProduct().getGrossUnitWeightKg())).toList());
        java.math.BigDecimal totalWeight = shares.stream().map(
                mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.Share::lineWeightKg)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String summary = java.util.stream.IntStream.range(0, lines.size()).mapToObj(index -> {
            DeliveryGuideLine line = lines.get(index);
            var share = shares.get(index);
            int factor = Math.max(1, line.getProduct().getUnitsPerBox());
            return line.getProduct().getName() + ": "
                    + mz.multicore.erp.architecture.quantity.PackageQuantity.label(line.getQuantity(), factor)
                    + ", " + share.lineWeightKg() + " kg"
                    + " (" + share.quantityPercentage() + "% qtd; " + share.weightPercentage() + "% peso)";
        }).collect(java.util.stream.Collectors.joining(" | "));
        Paragraph paragraph = new Paragraph("Carga total: " + totalWeight + " kg. Volumes: " + summary,
                PdfTheme.smallFont());
        paragraph.setSpacingBefore(5f);
        return paragraph;
    }
}

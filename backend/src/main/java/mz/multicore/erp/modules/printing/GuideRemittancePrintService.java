package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera a Guia de Remessa (delivery note) a partir de uma fatura — acompanha a mercadoria expedida.
 * Reutiliza o cabeçalho da empresa, o bloco de cliente/entrega e o renderizador de linhas partilhado
 * (mesmas colunas dos restantes documentos), acrescentando bloco de transporte e assinaturas.
 * Documento não fiscal de pagamento: a referência é derivada da fatura, sem consumir nova numeração.
 */
@Service
public class GuideRemittancePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InvoiceRepository invoiceRepository;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;
    private final mz.multicore.erp.modules.comercial.repository.OrderRepository orderRepository;

    public GuideRemittancePrintService(InvoiceRepository invoiceRepository, LineRowMapper lineRowMapper,
                                       DocumentConfigService documentConfigService) {
        this(invoiceRepository, lineRowMapper, documentConfigService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GuideRemittancePrintService(InvoiceRepository invoiceRepository, LineRowMapper lineRowMapper,
                                       DocumentConfigService documentConfigService,
                                       mz.multicore.erp.modules.comercial.repository.OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
        this.orderRepository = orderRepository;
    }

    public static final List<String> GUIDE_REMITTANCE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Destinatário / Cliente)",
            "2.ª VIA — DUPLICADO (Trânsito e Fiscalização / Viatura)",
            "3.ª VIA — TRIPLICADO (Expedição / Arquivo de Origem)",
            "4.ª VIA — QUADRUPLICADO (Comprovativo de Recepção e Devolução)"
    );

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());

        List<LineItemsTableRenderer.Row> rows = toRows(invoice.getLines());
        var rawColumns = documentConfigService.getColumns(invoice.getCompany().getId(),
                mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL);
        // Na Guia de Remessa, assegurar sempre visibilidade de Valor Unitário e Subtotal
        var columns = new mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO(
                rawColumns.barcode(),
                rawColumns.reference(),
                rawColumns.description(),
                rawColumns.expiry(),
                rawColumns.quantity(),
                rawColumns.packages(),
                rawColumns.boxes(),
                rawColumns.boxPercentage(),
                true, // unitPrice
                rawColumns.tax(),
                true, // subtotal
                rawColumns.footer()
        );

        return PdfDocumentBuilder.buildA4(doc -> {
            for (int i = 0; i < GUIDE_REMITTANCE_COPIES.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderGuideCopy(doc, invoice, GUIDE_REMITTANCE_COPIES.get(i), rows, columns);
            }
        });
    }

    private void renderGuideCopy(com.lowagie.text.Document doc, Invoice invoice, String copyLabel,
                                 List<LineItemsTableRenderer.Row> rows,
                                 mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO columns) {
        doc.add(CompanyHeaderRenderer.build(
                invoice.getCompany(),
                "Guia de Remessa",
                guideNumber(invoice)
        ));
        doc.add(buildCopyBadge(copyLabel));
        doc.add(buildDeliveryBlock(invoice));
        doc.add(LineItemsTableRenderer.build(rows, columns));
        
        // Totais e Valor da Mercadoria
        java.math.BigDecimal subtotal = invoice.getTotalBeforeTax() != null ? invoice.getTotalBeforeTax() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : subtotal.add(tax);
        doc.add(TotalsBlockRenderer.build(subtotal, tax, total));

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
                .map(line -> {
                    InvoiceLine l = line;
                    mz.multicore.erp.modules.comercial.model.Product p = l.getProduct();
                    java.math.BigDecimal weight = null;
                    int unitsPerBox = 1;
                    String name = "Artigo";
                    if (p != null) {
                        weight = p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg();
                        unitsPerBox = p.getUnitsPerBox();
                        name = p.getName();
                    }
                    return new LoadSummaryRenderer.Item(name, l.getQuantity(), weight, unitsPerBox);
                })
                .toList());
    }

    private static Paragraph buildStockNotice(Invoice invoice) {
        com.lowagie.text.Font noticeFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 8f, com.lowagie.text.Font.ITALIC, new java.awt.Color(71, 85, 105));
        Paragraph p = new Paragraph("Nota: Documento exclusivamente de transporte e trânsito com valor declarado para fiscalização — "
                + "Não movimenta stock (saída de mercadoria efectuada na Fatura " + invoice.getInvoiceNumber() + ").", noticeFont);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(4f);
        return p;
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

    /** Referência determinística da guia, derivada da fatura — não consome numeração ao reimprimir. */
    private String guideNumber(Invoice invoice) {
        return "GR-" + invoice.getInvoiceNumber();
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

        java.math.BigDecimal totalWeight = java.math.BigDecimal.ZERO;
        int totalPackages = 0;
        if (invoice.getLines() != null) {
            for (InvoiceLine l : invoice.getLines()) {
                mz.multicore.erp.modules.comercial.model.Product p = l.getProduct();
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
                l.getProduct(),
                l.getBatchNumber(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }
}

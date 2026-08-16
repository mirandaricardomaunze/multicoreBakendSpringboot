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

    public GuideRemittancePrintService(InvoiceRepository invoiceRepository, LineRowMapper lineRowMapper,
                                       DocumentConfigService documentConfigService) {
        this.invoiceRepository = invoiceRepository;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    invoice.getCompany(),
                    "Guia de Remessa",
                    guideNumber(invoice)
            ));
            doc.add(buildDeliveryBlock(invoice));
            doc.add(LineItemsTableRenderer.build(toRows(invoice.getLines()),
                    documentConfigService.getColumns(invoice.getCompany().getId(), mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL)));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildTransportBlock());
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(buildSignatureBlock());
        });
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
        addRight(meta, "Documento de origem: " + invoice.getInvoiceNumber());
        if (invoice.getCreatedAt() != null) {
            addRight(meta, "Data: " + invoice.getCreatedAt().format(DATE_FMT));
        }
        if (invoice.getWarehouse() != null) {
            addRight(meta, "Origem (armazém): " + invoice.getWarehouse().getName());
        }
        table.addCell(meta);
        return table;
    }

    private PdfPTable buildTransportBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Transporte", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph("Transportador: ____________________________", PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Matrícula: ________________________________", PdfTheme.bodyFont()));
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

package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.CreditNoteLine;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for a credit note (Nota de Crédito). */
@Service
public class CreditNotePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final List<String> CREDIT_NOTE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Cliente)",
            "2.ª VIA — DUPLICADO (Contabilidade / Fisco)",
            "3.ª VIA — TRIPLICADO (Arquivo / Cobrança)",
            "4.ª VIA — QUADRUPLICADO (Arquivo Geral)"
    );

    private final CreditNoteService service;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public CreditNotePrintService(CreditNoteService service, LineRowMapper lineRowMapper,
                                  DocumentConfigService documentConfigService) {
        this.service = service;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long noteId) {
        CreditNote note = service.loadForPrint(noteId);
        DocumentTemplateDTO template = documentConfigService.getTemplate(note.getCompany().getId(),
                mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL);

        return PdfDocumentBuilder.buildA4(doc -> {
            for (int i = 0; i < CREDIT_NOTE_COPIES.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderCreditNoteCopy(doc, note, CREDIT_NOTE_COPIES.get(i), template);
            }
        });
    }

    private void renderCreditNoteCopy(com.lowagie.text.Document doc, CreditNote note, String copyLabel,
                                      DocumentTemplateDTO template) {
        doc.add(CompanyHeaderRenderer.build(
                note.getCompany(),
                "Nota de Crédito",
                note.getNoteNumber(),
                copyLabel,
                null,
                template
        ));
        List<LineItemsTableRenderer.Row> rows = toRows(note.getLines());
        doc.add(buildContextBlock(note));
        doc.add(LineItemsTableRenderer.buildCompact(rows, documentConfigService.getColumns(
                note.getCompany().getId(), mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL)));
        doc.add(PdfDocumentBuilder.spacer(6f));
        doc.add(TotalsBlockRenderer.build(
                note.getTotalBeforeTax(),
                note.getTaxAmount(),
                note.getTotalAmount(),
                TotalsBlockRenderer.calculateBreakdowns(rows),
                "TOTAL CREDITADO"
        ));
        if (note.getDescription() != null && !note.getDescription().isBlank()) {
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(new Paragraph("Observações: " + note.getDescription(), PdfTheme.bodyFont()));
        }
        DocumentTemplateBlocks.addA4Closing(doc, template);
        doc.add(PdfDocumentBuilder.spacer(24f));
        doc.add(SignatureBlockRenderer.build("Entidade Emitente", "Cliente"));
    }

    private PdfPTable buildContextBlock(CreditNote note) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Cliente", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(FiscalElements.buyerName(null, note.getClient()), PdfTheme.bodyFont()));
        left.addElement(new Paragraph("NUIT: " + FiscalElements.buyerTaxId(note.getClient()), PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Fatura: " + note.getInvoice().getInvoiceNumber(), PdfTheme.bodyFont()));
        if (note.getWarehouse() != null) {
            left.addElement(new Paragraph("Armazém devolução: " + note.getWarehouse().getName(), PdfTheme.smallFont()));
        }
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph date = new Paragraph("Data: " + note.getIssueDate().format(DATE_FMT), PdfTheme.bodyFont());
        date.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(date);
        Paragraph reason = new Paragraph("Motivo: " + DocumentLabels.reason(note.getReason()), PdfTheme.bodyFont());
        reason.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(reason);
        Paragraph status = new Paragraph("Estado: " + DocumentLabels.status(note.getStatus()), PdfTheme.boldFont());
        status.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(status);
        if (note.getApprovedBy() != null) {
            Paragraph approved = new Paragraph("Aprovado por: " + note.getApprovedBy(), PdfTheme.smallFont());
            approved.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(approved);
        }
        table.addCell(right);
        return table;
    }

    private List<LineItemsTableRenderer.Row> toRows(List<CreditNoteLine> lines) {
        return lines.stream().map(l -> lineRowMapper.map(
                l.getProduct(),
                l.getBatchNumber(),
                l.getQuantity() == null ? BigDecimal.ZERO : l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal(),
                l.getInvoiceLine() == null ? null : l.getInvoiceLine().effectiveExemptionReason()
        )).toList();
    }
}

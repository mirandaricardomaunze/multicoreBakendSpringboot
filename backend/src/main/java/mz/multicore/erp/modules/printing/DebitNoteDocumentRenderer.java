package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DebitNoteLine;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DebitNoteDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DocumentConfigService documentConfigService;

    /** {@code documentConfigService} nulo (testes sem Spring) imprime com o modelo por omissão. */
    public DebitNoteDocumentRenderer(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    /** Usa o modelo actual da empresa, também nas 2.ª vias de notas antigas (SPEC §3.3). */
    public byte[] render(DebitNote note, List<String> copies) {
        return render(note, copies, documentConfigService == null ? null
                : documentConfigService.getTemplate(note.getCompany().getId(), DocumentType.COMMERCIAL));
    }

    /** Vias A4 com o modelo actual da empresa (DT-04). */
    public byte[] render(DebitNote note, List<String> copies, DocumentTemplateDTO template) {
        return PdfDocumentBuilder.buildA4(doc -> {
            for (int i = 0; i < copies.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderDebitNoteCopy(doc, note, copies.get(i), template);
            }
        });
    }

    private void renderDebitNoteCopy(com.lowagie.text.Document doc, DebitNote note, String copyLabel,
                                     DocumentTemplateDTO template) {
        doc.add(CompanyHeaderRenderer.build(note.getCompany(), "Nota de Débito", note.getNoteNumber(), copyLabel,
                null, template));
        doc.add(buildContextBlock(note));
        doc.add(buildLinesTable(note.getLines()));
        doc.add(PdfDocumentBuilder.spacer(6f));
        doc.add(TotalsBlockRenderer.build(
                note.getTotalBeforeTax(),
                note.getTaxAmount(),
                note.getTotalAmount(),
                TotalsBlockRenderer.calculateBreakdowns(note.getLines().stream().map(l -> FiscalElements.taxRow(
                        BigDecimal.ONE, l.getAmount(), l.getTaxRate(), BigDecimal.ZERO, l.getAmount(), null)).toList()),
                "TOTAL DEBITADO"
        ));
        if (note.getDescription() != null && !note.getDescription().isBlank()) {
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(new Paragraph("Observações: " + note.getDescription(), PdfTheme.bodyFont()));
        }
        DocumentTemplateBlocks.addA4Closing(doc, template);
        doc.add(PdfDocumentBuilder.spacer(24f));
        doc.add(SignatureBlockRenderer.build("Entidade Emitente", "Cliente"));
    }

    private PdfPTable buildContextBlock(DebitNote note) {
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
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addRight(right, "Data: " + note.getIssueDate().format(DATE_FMT), PdfTheme.bodyFont());
        addRight(right, "Motivo: " + DocumentLabels.reason(note.getReason()), PdfTheme.bodyFont());
        addRight(right, "Estado: " + DocumentLabels.status(note.getStatus()), PdfTheme.boldFont());
        if (note.getApprovedBy() != null) {
            addRight(right, "Aprovado por: " + note.getApprovedBy(), PdfTheme.smallFont());
        }
        table.addCell(right);
        return table;
    }

    private void addRight(PdfPCell cell, String text, com.lowagie.text.Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(paragraph);
    }

    private PdfPTable buildLinesTable(List<DebitNoteLine> lines) {
        PdfPTable table = new PdfPTable(new float[]{50f, 20f, 10f, 20f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        header(table, "Descrição", Element.ALIGN_LEFT);
        header(table, "Valor (s/ IVA)", Element.ALIGN_RIGHT);
        header(table, "IVA", Element.ALIGN_RIGHT);
        header(table, "Total", Element.ALIGN_RIGHT);

        for (DebitNoteLine l : lines) {
            body(table, l.getDescription(), Element.ALIGN_LEFT);
            body(table, MoneyFormat.formatPlain(l.getAmount()), Element.ALIGN_RIGHT);
            body(table, formatRate(l.getTaxRate()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(l.getLineTotal()), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        cell.setNoWrap(true);
        table.addCell(cell);
    }

    private void body(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private String formatRate(BigDecimal rate) {
        return rate == null ? "0%" : rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString().replace('.', ',') + "%";
    }
}

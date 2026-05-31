package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.modules.comercial.model.DebitNote;
import com.phcpro.modules.comercial.model.DebitNoteLine;
import com.phcpro.modules.comercial.service.DebitNoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for a debit note (Nota de Débito). */
@Service
public class DebitNotePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DebitNoteService service;

    public DebitNotePrintService(DebitNoteService service) {
        this.service = service;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long noteId) {
        DebitNote note = service.loadForPrint(noteId);

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    note.getCompany(),
                    "Nota de Débito",
                    note.getNoteNumber()
            ));
            doc.add(buildContextBlock(note));
            doc.add(buildLinesTable(note.getLines()));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(TotalsBlockRenderer.build(
                    note.getTotalBeforeTax(),
                    note.getTaxAmount(),
                    note.getTotalAmount()
            ));
            if (note.getDescription() != null && !note.getDescription().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(10f));
                doc.add(new Paragraph("Observações: " + note.getDescription(), PdfTheme.bodyFont()));
            }
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildContextBlock(DebitNote note) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Cliente", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(note.getClient().getName(), PdfTheme.bodyFont()));
        if (note.getClient().getTaxId() != null) {
            left.addElement(new Paragraph("NUIT: " + note.getClient().getTaxId(), PdfTheme.bodyFont()));
        }
        left.addElement(new Paragraph("Fatura: " + note.getInvoice().getInvoiceNumber(), PdfTheme.bodyFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph date = new Paragraph("Data: " + note.getIssueDate().format(DATE_FMT), PdfTheme.bodyFont());
        date.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(date);
        Paragraph reason = new Paragraph("Motivo: " + note.getReason().name(), PdfTheme.bodyFont());
        reason.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(reason);
        Paragraph status = new Paragraph("Estado: " + note.getStatus().name(), PdfTheme.boldFont());
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

    private PdfPTable buildLinesTable(List<DebitNoteLine> lines) {
        PdfPTable table = new PdfPTable(new float[]{60f, 15f, 25f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        header(table, "Descrição", Element.ALIGN_LEFT);
        header(table, "IVA", Element.ALIGN_RIGHT);
        header(table, "Total", Element.ALIGN_RIGHT);

        for (DebitNoteLine l : lines) {
            body(table, l.getDescription(), Element.ALIGN_LEFT);
            body(table, formatRate(l.getTaxRate()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(l.getLineTotal()), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Entidade Emitente"));
        table.addCell(signatureCell("Cliente"));
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

    private void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void body(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private String formatRate(java.math.BigDecimal rate) {
        if (rate == null) return "0%";
        java.math.BigDecimal percent = rate.multiply(java.math.BigDecimal.valueOf(100));
        return percent.stripTrailingZeros().toPlainString() + "%";
    }
}

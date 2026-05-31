package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.modules.comercial.model.CreditNote;
import com.phcpro.modules.comercial.model.CreditNoteLine;
import com.phcpro.modules.comercial.service.CreditNoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for a credit note (Nota de Crédito). */
@Service
public class CreditNotePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CreditNoteService service;

    public CreditNotePrintService(CreditNoteService service) {
        this.service = service;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long noteId) {
        CreditNote note = service.loadForPrint(noteId);

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    note.getCompany(),
                    "Nota de Crédito",
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

    private PdfPTable buildContextBlock(CreditNote note) {
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

    private PdfPTable buildLinesTable(List<CreditNoteLine> lines) {
        return LineItemsTableRenderer.build(lines.stream().map(l -> new LineItemsTableRenderer.Row(
                l.getProduct().getSku(),
                l.getProduct().getName() + (l.getBatchNumber() != null ? "  (Lote: " + l.getBatchNumber() + ")" : ""),
                l.getQuantity() == null ? 0 : l.getQuantity().intValue(),
                l.getUnitPrice(),
                l.getTaxRate(),
                BigDecimal.ZERO,
                l.getLineTotal()
        )).toList());
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
}

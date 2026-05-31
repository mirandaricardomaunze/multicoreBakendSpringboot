package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.modules.inventory.model.StockTransfer;
import com.phcpro.modules.inventory.model.StockTransferLine;
import com.phcpro.modules.inventory.service.StockTransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for an inter-warehouse stock transfer note (Guia de Transferência). */
@Service
public class StockTransferPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StockTransferService stockTransferService;

    public StockTransferPrintService(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long transferId) {
        StockTransfer transfer = stockTransferService.loadForPrint(transferId);

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    transfer.getCompany(),
                    "Guia de Transferência",
                    transfer.getTransferNumber()
            ));
            doc.add(buildRouteBlock(transfer));
            doc.add(buildLinesTable(transfer.getLines()));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildTotalsLine(transfer.getLines()));
            if (transfer.getNotes() != null && !transfer.getNotes().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(8f));
                Paragraph notes = new Paragraph("Observações: " + transfer.getNotes(), PdfTheme.bodyFont());
                doc.add(notes);
            }
            doc.add(PdfDocumentBuilder.spacer(30f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildRouteBlock(StockTransfer transfer) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell origin = new PdfPCell();
        origin.setBorder(PdfPCell.NO_BORDER);
        origin.addElement(new Paragraph("Armazém de Origem", PdfTheme.subtitleFont()));
        origin.addElement(new Paragraph(transfer.getOriginWarehouse().getName(), PdfTheme.bodyFont()));
        if (transfer.getOriginWarehouse().getLocation() != null) {
            origin.addElement(new Paragraph(transfer.getOriginWarehouse().getLocation(), PdfTheme.smallFont()));
        }
        if (transfer.getResponsible() != null) {
            origin.addElement(new Paragraph("Responsável: " + transfer.getResponsible(), PdfTheme.bodyFont()));
        }
        if (transfer.getVehicle() != null) {
            origin.addElement(new Paragraph("Veículo: " + transfer.getVehicle(), PdfTheme.bodyFont()));
        }
        table.addCell(origin);

        PdfPCell destination = new PdfPCell();
        destination.setBorder(PdfPCell.NO_BORDER);
        destination.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph destTitle = new Paragraph("Armazém de Destino", PdfTheme.subtitleFont());
        destTitle.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(destTitle);
        Paragraph destName = new Paragraph(transfer.getDestinationWarehouse().getName(), PdfTheme.bodyFont());
        destName.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(destName);
        if (transfer.getDestinationWarehouse().getLocation() != null) {
            Paragraph loc = new Paragraph(transfer.getDestinationWarehouse().getLocation(), PdfTheme.smallFont());
            loc.setAlignment(Element.ALIGN_RIGHT);
            destination.addElement(loc);
        }
        Paragraph date = new Paragraph("Data: " + transfer.getTransferDate().format(DATE_FMT), PdfTheme.bodyFont());
        date.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(date);
        Paragraph status = new Paragraph("Estado: " + transfer.getStatus(), PdfTheme.boldFont());
        status.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(status);
        table.addCell(destination);

        return table;
    }

    private PdfPTable buildLinesTable(List<StockTransferLine> lines) {
        PdfPTable table = new PdfPTable(new float[]{15f, 50f, 20f, 15f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        header(table, "Código", Element.ALIGN_LEFT);
        header(table, "Descrição", Element.ALIGN_LEFT);
        header(table, "Lote(s)", Element.ALIGN_LEFT);
        header(table, "Qtd", Element.ALIGN_RIGHT);

        for (StockTransferLine l : lines) {
            body(table, l.getProduct().getSku(), Element.ALIGN_LEFT);
            body(table, l.getProduct().getName(), Element.ALIGN_LEFT);
            body(table, l.getBatchNumber() == null ? "-" : l.getBatchNumber(), Element.ALIGN_LEFT);
            body(table, formatQty(l.getQuantity()), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private PdfPTable buildTotalsLine(List<StockTransferLine> lines) {
        BigDecimal total = lines.stream()
                .map(StockTransferLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lineCount = lines.size();

        PdfPTable wrapper = new PdfPTable(new float[]{60f, 40f});
        wrapper.setWidthPercentage(100);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});
        addTotalsLine(inner, "Linhas", String.valueOf(lineCount), false);
        addTotalsLine(inner, "Qtd. Total", formatQty(total), true);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        table.addCell(signatureCell("Entregue por (Origem)"));
        table.addCell(signatureCell("Recebido por (Destino)"));
        return table;
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);

        Paragraph line = new Paragraph("____________________________", PdfTheme.bodyFont());
        Paragraph caption = new Paragraph(label, PdfTheme.smallFont());
        cell.addElement(line);
        cell.addElement(caption);
        return cell;
    }

    private void addTotalsLine(PdfPTable t, String label, String value, boolean emphasised) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(value, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        l.setBorder(PdfPCell.NO_BORDER);
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3f);
        v.setPadding(3f);
        if (emphasised) {
            l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
            v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        t.addCell(l);
        t.addCell(v);
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
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private String formatQty(BigDecimal qty) {
        if (qty == null) return "0";
        return String.format("%,.3f", qty);
    }
}

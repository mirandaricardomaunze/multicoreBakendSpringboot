package mz.multicore.erp.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReceiptDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] renderDocument(Invoice invoice, DocumentColumnsDTO cols, List<PaymentEntry> entries) {
        return PdfDocumentBuilder.buildReceipt(doc -> {
            renderHeader(doc, invoice);
            renderLines(doc, invoice, cols);
            renderTotals(doc, invoice);
            renderPayments(doc, entries);
            renderFooter(doc, cols);
        });
    }

    private void renderHeader(Document doc, Invoice invoice) {
        ThermalReceiptRenderer.companyHeader(doc, invoice.getCompany());
        addCentered(doc, "RECIBO POS", PdfTheme.boldFont());
        addCentered(doc, "1.ª VIA — ORIGINAL (CLIENTE)", PdfTheme.smallFont());
        addCentered(doc, invoice.getInvoiceNumber(), PdfTheme.bodyFont());
        addCentered(doc, invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(DATE_FMT) : "", PdfTheme.smallFont());
        addCentered(doc, "Cliente: " + buyerName(invoice), PdfTheme.smallFont());
        addCentered(doc, "Operador: " + (invoice.getCreatedBy() != null ? invoice.getCreatedBy() : "—"), PdfTheme.smallFont());
        doc.add(PdfDocumentBuilder.spacer(4f));
    }

    private void renderLines(Document doc, Invoice invoice, DocumentColumnsDTO cols) {
        PdfPTable table = new PdfPTable(new float[]{65f, 35f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        addCell(table, "Artigo", PdfTheme.tableHeaderFont(), Element.ALIGN_LEFT, true);
        addCell(table, "Total", PdfTheme.tableHeaderFont(), Element.ALIGN_RIGHT, true);
        for (InvoiceLine line : invoice.getLines()) {
            addDescriptionCell(table, line, cols);
            addCell(table, MoneyFormat.formatPlain(line.getLineTotal()), PdfTheme.boldFont(), Element.ALIGN_RIGHT, false);
        }
        doc.add(table);
    }

    private void addDescriptionCell(PdfPTable table, InvoiceLine line, DocumentColumnsDTO cols) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(line.getProduct().getName(), PdfTheme.bodyFont()));
        String sub = null;
        String ref = line.getProduct().getReference();
        String barcode = line.getProduct().getBarcode();
        if (cols.reference() && ref != null && !ref.isBlank()) {
            sub = "Ref: " + ref;
        } else if (cols.barcode() && barcode != null && !barcode.isBlank()) {
            sub = barcode;
        }
        if (sub != null) phrase.add(new Chunk("\n" + sub, PdfTheme.smallFont()));
        String details = lineDetailsLabel(line, cols);
        if (!details.isBlank()) phrase.add(new Chunk("\n" + details, PdfTheme.smallFont()));
        phrase.add(new Chunk("\n" + vatRateLabel(line.getTaxRate()), PdfTheme.smallFont()));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(ThermalReceiptRenderer.DOTTED_BOTTOM);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private void renderTotals(Document doc, Invoice invoice) {
        addDottedLine(doc);
        addRight(doc, "Subtotal: " + MoneyFormat.format(invoice.getTotalBeforeTax()), PdfTheme.bodyFont());
        addRight(doc, "IVA:      " + MoneyFormat.format(invoice.getTaxAmount()), PdfTheme.bodyFont());
        addRight(doc, "TOTAL:    " + MoneyFormat.format(invoice.getTotalAmount()), PdfTheme.boldFont());
    }

    public static String vatRateLabel(BigDecimal rate) {
        BigDecimal effective = rate == null ? BigDecimal.ZERO : rate;
        if (effective.signum() == 0) return "IVA: Isento";
        String percent = effective.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
        return "IVA: " + percent + "%";
    }

    public static String lineDetailsLabel(InvoiceLine line, DocumentColumnsDTO cols) {
        if (!cols.quantity() && !cols.unitPrice()) return "";
        String quantity = line.getQuantity() == null ? "0" : line.getQuantity().stripTrailingZeros().toPlainString();
        if (cols.quantity() && cols.unitPrice()) {
            return quantity + " x " + MoneyFormat.formatPlain(line.getUnitPrice()) + " MT";
        }
        if (cols.quantity()) return "Qtd: " + quantity;
        return "Preço: " + MoneyFormat.formatPlain(line.getUnitPrice()) + " MT";
    }

    private void renderPayments(Document doc, List<PaymentEntry> entries) {
        if (entries.isEmpty()) return;
        addDottedLine(doc);
        BigDecimal totalTendered = BigDecimal.ZERO;
        BigDecimal totalChange = BigDecimal.ZERO;
        for (PaymentEntry e : entries) {
            addRight(doc, methodLabel(e.getMethod()) + ": " + MoneyFormat.format(e.getAmount()), PdfTheme.bodyFont());
            if (e.getTenderedAmount() != null) totalTendered = totalTendered.add(e.getTenderedAmount());
            if (e.getChangeGiven() != null) totalChange = totalChange.add(e.getChangeGiven());
        }
        if (totalChange.compareTo(BigDecimal.ZERO) > 0) {
            addRight(doc, "Valor pago: " + MoneyFormat.format(totalTendered), PdfTheme.bodyFont());
            addRight(doc, "Troco:      " + MoneyFormat.format(totalChange), PdfTheme.boldFont());
        }
    }

    private String buyerName(Invoice invoice) {
        if (invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank()) return invoice.getCustomerName();
        return invoice.getClient() != null ? invoice.getClient().getName() : "—";
    }

    private String methodLabel(PaymentMethod method) {
        if (method == null) return "Pago";
        return switch (method) {
            case CASH -> "Numerário";
            case CARD -> "Cartão";
            case BANK_TRANSFER -> "Transferência";
            case MPESA -> "M-Pesa";
            case EMOLA -> "e-Mola";
            case CREDIT -> "Fiado";
            case STORE_CREDIT -> "Vale de Compras";
        };
    }

    private void renderFooter(Document doc, DocumentColumnsDTO cols) {
        doc.add(PdfDocumentBuilder.spacer(6f));
        String message = (cols.footer() != null && !cols.footer().isBlank()) ? cols.footer() : "Obrigado pela sua preferência!";
        for (String line : message.split("\\r?\\n")) {
            addCentered(doc, line, PdfTheme.smallFont());
        }
    }

    private void addCentered(Document doc, String text, Font font) {
        ThermalReceiptRenderer.centered(doc, text, font);
    }

    private void addRight(Document doc, String text, Font font) {
        ThermalReceiptRenderer.right(doc, text, font);
    }

    private void addCell(PdfPTable table, String text, Font font, int align, boolean header) {
        ThermalReceiptRenderer.cell(table, text, font, align, header);
    }

    private void addDottedLine(Document doc) {
        ThermalReceiptRenderer.dottedLine(doc);
    }
}

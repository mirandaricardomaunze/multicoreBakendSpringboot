package com.phcpro.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Generates a compact (~80mm) PDF receipt for a POS sale.
 * Single responsibility: turn one Invoice into a printable receipt.
 */
@Service
public class ReceiptPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InvoiceRepository invoiceRepository;

    public ReceiptPrintService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada para emissão de recibo."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        return PdfDocumentBuilder.buildReceipt(doc -> {
            renderHeader(doc, invoice);
            renderLines(doc, invoice);
            renderTotals(doc, invoice);
            renderFooter(doc, invoice);
        });
    }

    private void renderHeader(Document doc, Invoice invoice) {
        Company company = invoice.getCompany();
        Paragraph name = new Paragraph(company == null ? "Empresa" : company.getName(), PdfTheme.subtitleFont());
        name.setAlignment(Element.ALIGN_CENTER);
        doc.add(name);

        if (company != null) {
            if (company.getTaxId() != null) addCentered(doc, "NUIT: " + company.getTaxId(), PdfTheme.smallFont());
            if (company.getAddress() != null) addCentered(doc, company.getAddress(), PdfTheme.smallFont());
        }
        addCentered(doc, "—————————————————", PdfTheme.smallFont());

        addCentered(doc, "RECIBO POS", PdfTheme.boldFont());
        addCentered(doc, invoice.getInvoiceNumber(), PdfTheme.bodyFont());
        addCentered(doc, invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(DATE_FMT) : "", PdfTheme.smallFont());
        addCentered(doc, "Cliente: " + (invoice.getClient() != null ? invoice.getClient().getName() : "—"), PdfTheme.smallFont());
        addCentered(doc, "Operador: " + (invoice.getCreatedBy() != null ? invoice.getCreatedBy() : "—"), PdfTheme.smallFont());

        doc.add(PdfDocumentBuilder.spacer(4f));
    }

    private void renderLines(Document doc, Invoice invoice) {
        PdfPTable table = new PdfPTable(new float[]{56f, 18f, 26f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);

        addCell(table, "Descrição", PdfTheme.tableHeaderFont(), Element.ALIGN_LEFT, true);
        addCell(table, "Qtd",       PdfTheme.tableHeaderFont(), Element.ALIGN_RIGHT, true);
        addCell(table, "Total",     PdfTheme.tableHeaderFont(), Element.ALIGN_RIGHT, true);

        for (InvoiceLine line : invoice.getLines()) {
            addCell(table, line.getProduct().getName(), PdfTheme.bodyFont(), Element.ALIGN_LEFT, false);
            addCell(table, String.valueOf(line.getQuantity()), PdfTheme.bodyFont(), Element.ALIGN_RIGHT, false);
            addCell(table, MoneyFormat.formatPlain(line.getLineTotal()), PdfTheme.bodyFont(), Element.ALIGN_RIGHT, false);
        }
        doc.add(table);
    }

    private void renderTotals(Document doc, Invoice invoice) {
        addCentered(doc, "—————————————————", PdfTheme.smallFont());
        addRight(doc, "Subtotal: " + MoneyFormat.format(invoice.getTotalBeforeTax()), PdfTheme.bodyFont());
        addRight(doc, "IVA:      " + MoneyFormat.format(invoice.getTaxAmount()), PdfTheme.bodyFont());
        addRight(doc, "TOTAL:    " + MoneyFormat.format(invoice.getTotalAmount()), PdfTheme.boldFont());
    }

    private void renderFooter(Document doc, Invoice invoice) {
        doc.add(PdfDocumentBuilder.spacer(6f));
        addCentered(doc, "Obrigado pela sua preferência!", PdfTheme.smallFont());
    }

    private void addCentered(Document doc, String text, com.lowagie.text.Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private void addRight(Document doc, String text, com.lowagie.text.Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        doc.add(p);
    }

    private void addCell(PdfPTable table, String text, com.lowagie.text.Font font, int align, boolean header) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(3f);
        if (header) cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        table.addCell(cell);
    }
}

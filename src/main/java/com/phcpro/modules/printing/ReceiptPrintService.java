package com.phcpro.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.pos.model.PaymentEntry;
import com.phcpro.modules.pos.model.PaymentMethod;
import com.phcpro.modules.pos.repository.PaymentEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a compact (~80mm) PDF receipt for a POS sale.
 * Single responsibility: turn one Invoice into a printable receipt.
 */
@Service
public class ReceiptPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InvoiceRepository invoiceRepository;
    private final PaymentEntryRepository paymentEntryRepository;

    public ReceiptPrintService(InvoiceRepository invoiceRepository,
                               PaymentEntryRepository paymentEntryRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentEntryRepository = paymentEntryRepository;
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
            renderPayments(doc, invoice);
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
        addDottedLine(doc);

        addCentered(doc, "RECIBO POS", PdfTheme.boldFont());
        addCentered(doc, invoice.getInvoiceNumber(), PdfTheme.bodyFont());
        addCentered(doc, invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(DATE_FMT) : "", PdfTheme.smallFont());
        addCentered(doc, "Cliente: " + buyerName(invoice), PdfTheme.smallFont());
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
        addDottedLine(doc);
        addRight(doc, "Subtotal: " + MoneyFormat.format(invoice.getTotalBeforeTax()), PdfTheme.bodyFont());
        addRight(doc, "IVA:      " + MoneyFormat.format(invoice.getTaxAmount()), PdfTheme.bodyFont());
        addRight(doc, "TOTAL:    " + MoneyFormat.format(invoice.getTotalAmount()), PdfTheme.boldFont());
    }

    /**
     * Bloco de pagamento: o que foi pago por cada método e, em numerário, o valor entregue
     * pelo cliente e o respectivo troco. Lê os {@link PaymentEntry} associados à fatura.
     */
    private void renderPayments(Document doc, Invoice invoice) {
        List<PaymentEntry> entries = paymentEntryRepository.findByInvoiceIdOrderByPaidAtAsc(invoice.getId());
        if (entries.isEmpty()) return;

        addDottedLine(doc);

        BigDecimal totalTendered = BigDecimal.ZERO;
        BigDecimal totalChange = BigDecimal.ZERO;
        for (PaymentEntry e : entries) {
            addRight(doc, methodLabel(e.getMethod()) + ": " + MoneyFormat.format(e.getAmount()), PdfTheme.bodyFont());
            if (e.getTenderedAmount() != null) {
                totalTendered = totalTendered.add(e.getTenderedAmount());
            }
            if (e.getChangeGiven() != null) {
                totalChange = totalChange.add(e.getChangeGiven());
            }
        }

        // Valor entregue e troco só fazem sentido quando houve numerário entregue acima do pago.
        if (totalChange.compareTo(BigDecimal.ZERO) > 0) {
            addRight(doc, "Valor pago: " + MoneyFormat.format(totalTendered), PdfTheme.bodyFont());
            addRight(doc, "Troco:      " + MoneyFormat.format(totalChange), PdfTheme.boldFont());
        }
    }

    /** Nome do comprador a imprimir: prioriza o nome guardado na venda, recai no cliente. */
    private String buyerName(Invoice invoice) {
        if (invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank()) {
            return invoice.getCustomerName();
        }
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
        };
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
        cell.setBorder(Rectangle.NO_BORDER);   // borda inferior pontilhada desenhada pelo evento
        cell.setCellEvent(DOTTED_BOTTOM);
        cell.setPadding(3f);
        if (header) cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        table.addCell(cell);
    }

    /** Linha separadora pontilhada (estilo recibo térmico) a toda a largura. */
    private void addDottedLine(Document doc) {
        DottedLineSeparator separator = new DottedLineSeparator();
        separator.setGap(2.2f);
        separator.setLineWidth(0.7f);
        separator.setLineColor(PdfTheme.BORDER);
        Paragraph p = new Paragraph();
        p.setSpacingBefore(2f);
        p.setSpacingAfter(2f);
        p.add(new Chunk(separator));
        doc.add(p);
    }

    /** Desenha a borda inferior de cada célula a pontilhado, para o aspecto de recibo térmico. */
    private static final PdfPCellEvent DOTTED_BOTTOM = (cell, position, canvases) -> {
        PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
        canvas.saveState();
        canvas.setLineWidth(0.6f);
        canvas.setLineDash(1f, 2f, 0f);
        canvas.setColorStroke(PdfTheme.BORDER);
        canvas.moveTo(position.getLeft(), position.getBottom());
        canvas.lineTo(position.getRight(), position.getBottom());
        canvas.stroke();
        canvas.restoreState();
    };
}

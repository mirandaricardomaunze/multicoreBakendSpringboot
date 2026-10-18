package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
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
 * Generates a full A4 invoice PDF — company header, client block, line items,
 * totals. Single responsibility: render one Invoice as a fiscal-style PDF.
 */
@Service
public class InvoicePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final List<String> INVOICE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Cliente)",
            "2.ª VIA — DUPLICADO (Contabilidade / Fisco)",
            "3.ª VIA — TRIPLICADO (Arquivo / Cobrança)",
            "4.ª VIA — QUADRUPLICADO (Arquivo Geral)"
    );

    private final InvoiceRepository invoiceRepository;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public InvoicePrintService(InvoiceRepository invoiceRepository, LineRowMapper lineRowMapper,
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
        List<LineItemsTableRenderer.Row> rows = toRows(invoice.getLines());
        var columns = documentConfigService.getColumns(invoice.getCompany().getId(),
                mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL);

        return PdfDocumentBuilder.buildA4(doc -> {
            for (int i = 0; i < INVOICE_COPIES.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderInvoiceCopy(doc, invoice, INVOICE_COPIES.get(i), rows, columns);
            }
        });
    }

    @Transactional(readOnly = true)
    public byte[] renderThermal(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());

        return PdfDocumentBuilder.buildReceipt(doc -> {
            ThermalReceiptRenderer.companyHeader(doc, invoice.getCompany());

            ThermalReceiptRenderer.centered(doc, "FATURA SIMPLIFICADA", PdfTheme.boldFont());
            ThermalReceiptRenderer.centered(doc, "1.ª VIA — ORIGINAL (CLIENTE)", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, invoice.getInvoiceNumber(), PdfTheme.bodyFont());
            if (invoice.getCreatedAt() != null) {
                ThermalReceiptRenderer.centered(doc, "Data: " + invoice.getCreatedAt().format(DATE_FMT), PdfTheme.smallFont());
            }
            String buyer = invoice.getClient() != null ? invoice.getClient().getName() : invoice.getCustomerName();
            if (buyer != null && !buyer.isBlank()) {
                ThermalReceiptRenderer.centered(doc, "Cliente: " + buyer, PdfTheme.smallFont());
            }
            if (invoice.getClient() != null && invoice.getClient().getTaxId() != null) {
                ThermalReceiptRenderer.centered(doc, "NUIT Cliente: " + invoice.getClient().getTaxId(), PdfTheme.smallFont());
            }

            ThermalReceiptRenderer.dottedLine(doc);

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(new float[]{65f, 35f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(2f);

            ThermalReceiptRenderer.cell(table, "Artigo", PdfTheme.tableHeaderFont(), com.lowagie.text.Element.ALIGN_LEFT, true);
            ThermalReceiptRenderer.cell(table, "Total", PdfTheme.tableHeaderFont(), com.lowagie.text.Element.ALIGN_RIGHT, true);

            for (InvoiceLine line : invoice.getLines()) {
                com.lowagie.text.Phrase phrase = new com.lowagie.text.Phrase();
                phrase.add(new com.lowagie.text.Chunk(line.getProduct().getName(), PdfTheme.bodyFont()));
                String qtyPrice = line.getQuantity() + " un. × " + MoneyFormat.formatPlain(line.getUnitPrice());
                if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0) {
                    qtyPrice += " (-" + line.getDiscountPercentage().stripTrailingZeros().toPlainString() + "%)";
                }
                phrase.add(new com.lowagie.text.Chunk("\n" + qtyPrice, PdfTheme.smallFont()));
                com.lowagie.text.pdf.PdfPCell descCell = new com.lowagie.text.pdf.PdfPCell(phrase);
                descCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                descCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                descCell.setCellEvent(ThermalReceiptRenderer.DOTTED_BOTTOM);
                descCell.setPadding(3f);
                table.addCell(descCell);

                ThermalReceiptRenderer.cell(table, MoneyFormat.formatPlain(line.getLineTotal()), PdfTheme.boldFont(), com.lowagie.text.Element.ALIGN_RIGHT, false);
            }
            doc.add(table);

            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.right(doc, "Subtotal: " + MoneyFormat.format(invoice.getTotalBeforeTax()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "IVA:      " + MoneyFormat.format(invoice.getTaxAmount()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "TOTAL:    " + MoneyFormat.format(invoice.getTotalAmount()), PdfTheme.boldFont());

            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.centered(doc, "Os bens/serviços foram postos à disposição", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "na data da emissão do documento.", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "Processado por Computador / Multicore ERP", PdfTheme.smallFont());
        });
    }

    private void renderInvoiceCopy(com.lowagie.text.Document doc, Invoice invoice, String copyLabel,
                                   List<LineItemsTableRenderer.Row> rows,
                                   mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO columns) {
        doc.add(CompanyHeaderRenderer.build(
                invoice.getCompany(),
                "Fatura",
                invoice.getInvoiceNumber(),
                copyLabel,
                null
        ));
        doc.add(ClientBlockRenderer.build(invoice.getClient(), invoice.getCustomerName(),
                invoice.getCreatedAt(), invoice.getWarehouse()));
        doc.add(LineItemsTableRenderer.build(rows, columns));
        doc.add(TotalsBlockRenderer.build(
                invoice.getTotalBeforeTax(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                TotalsBlockRenderer.calculateBreakdowns(rows)
        ));
        doc.add(PdfDocumentBuilder.spacer(8f));
        Paragraph status = new Paragraph("Estado: " + invoice.getStatus(), PdfTheme.boldFont());
        doc.add(status);
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

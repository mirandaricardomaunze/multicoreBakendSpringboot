package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

        InvoiceFooterEvent footerEvent = new InvoiceFooterEvent(invoice.getCompany(), INVOICE_COPIES);

        return PdfDocumentBuilder.buildA4(footerEvent, doc -> {
            for (int i = 0; i < INVOICE_COPIES.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderInvoiceCopy(doc, invoice, INVOICE_COPIES.get(i), rows);
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

    private void renderInvoiceCopy(Document doc, Invoice invoice, String copyLabel,
                                   List<LineItemsTableRenderer.Row> rows) {
        doc.add(buildHeaderTable(invoice.getCompany(), invoice, copyLabel));
        doc.add(buildClientAndMetaTable(invoice));
        doc.add(LineItemsTableRenderer.buildModelTable(rows));
        BigDecimal totalDiscount = calculateTotalDiscount(invoice);
        doc.add(TotalsBlockRenderer.buildModelTotals(
                invoice.getTotalBeforeTax(),
                totalDiscount,
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                TotalsBlockRenderer.calculateBreakdowns(rows)
        ));
        doc.add(buildPaymentAndNotesTable(invoice));
        doc.add(buildLegalDisclaimer());
    }

    private static PdfPTable buildHeaderTable(Company company, Invoice invoice, String copyLabel) {
        PdfPTable table = new PdfPTable(new float[]{55f, 45f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);

        // Coluna da Empresa (Esquerda)
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(PdfPCell.NO_BORDER);

        if (company != null && company.getLogo() != null && company.getLogo().length > 0) {
            try {
                Image logo = Image.getInstance(company.getLogo());
                logo.scaleToFit(140f, 45f);
                logo.setSpacingAfter(4f);
                companyCell.addElement(logo);
            } catch (Exception ignored) {}
        }

        String compName = (company != null && company.getName() != null) ? company.getName().toUpperCase() : "EMPRESA";
        Paragraph nameP = new Paragraph(compName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, PdfTheme.BRAND));
        nameP.setSpacingAfter(2f);
        companyCell.addElement(nameP);

        if (company != null) {
            if (company.getAddress() != null && !company.getAddress().isBlank()) {
                companyCell.addElement(new Paragraph(company.getAddress().trim(), PdfTheme.bodyFont()));
            }
            String taxAndPhone = "NUIT: " + (company.getTaxId() != null ? company.getTaxId() : "—");
            if (company.getPhone() != null && !company.getPhone().isBlank()) {
                taxAndPhone += " · Tel.: " + company.getPhone().trim();
            }
            companyCell.addElement(new Paragraph(taxAndPhone, PdfTheme.bodyFont()));

            if (company.getEmail() != null && !company.getEmail().isBlank()) {
                companyCell.addElement(new Paragraph("E-mail: " + company.getEmail().trim(), PdfTheme.bodyFont()));
            }
        }
        table.addCell(companyCell);

        // Coluna do Documento (Direita)
        PdfPCell docCell = new PdfPCell();
        docCell.setBorder(PdfPCell.NO_BORDER);
        docCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph docTitle = new Paragraph("FATURA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, PdfTheme.BRAND));
        docTitle.setAlignment(Element.ALIGN_RIGHT);
        docCell.addElement(docTitle);

        String num = "N.º " + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "—");
        Paragraph numP = new Paragraph(num, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, PdfTheme.BRAND));
        numP.setAlignment(Element.ALIGN_RIGHT);
        numP.setSpacingAfter(2f);
        docCell.addElement(numP);

        Paragraph copyP = new Paragraph(copyLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PdfTheme.MUTED));
        copyP.setAlignment(Element.ALIGN_RIGHT);
        docCell.addElement(copyP);

        table.addCell(docCell);
        return table;
    }

    private static PdfPTable buildClientAndMetaTable(Invoice invoice) {
        PdfPTable table = new PdfPTable(new float[]{55f, 45f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);

        // FATURADO A (Esquerda)
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(PdfPCell.NO_BORDER);

        Paragraph faturadoA = new Paragraph("FATURADO A", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED));
        faturadoA.setSpacingAfter(2f);
        clientCell.addElement(faturadoA);

        Client client = invoice.getClient();
        String buyerName = invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank()
                ? invoice.getCustomerName().trim()
                : (client != null ? client.getName() : "Consumidor Final");
        Paragraph buyerP = new Paragraph(buyerName, PdfTheme.boldFont());
        buyerP.setSpacingAfter(2f);
        clientCell.addElement(buyerP);

        if (client != null && client.getAddress() != null && !client.getAddress().isBlank()
                && !"-".equals(client.getAddress()) && !"—".equals(client.getAddress())) {
            clientCell.addElement(new Paragraph(client.getAddress().trim(), PdfTheme.bodyFont()));
        }
        String clientTaxId = (client != null && client.getTaxId() != null && !client.getTaxId().isBlank())
                ? client.getTaxId().trim()
                : "Consumidor Final";
        clientCell.addElement(new Paragraph("NUIT: " + clientTaxId, PdfTheme.bodyFont()));

        table.addCell(clientCell);

        // Metadados (Direita - Grelha 2x2)
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(PdfPCell.NO_BORDER);

        PdfPTable grid = new PdfPTable(new float[]{50f, 50f});
        grid.setWidthPercentage(100);

        String issueDateStr = invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(DATE_FMT) : "—";
        LocalDate due = invoice.effectiveDueDate();
        String dueDateStr = due != null ? due.format(DATE_FMT) : issueDateStr;

        int termsDays = client != null ? client.effectivePaymentTermsDays() : 0;
        String condPag = termsDays == 0 ? "Pronto Pagamento" : termsDays + " dias";
        String refStr = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "—";

        addMetaBox(grid, "DATA DE EMISSÃO", issueDateStr);
        addMetaBox(grid, "VENCIMENTO", dueDateStr);
        addMetaBox(grid, "COND. PAGAMENTO", condPag);
        addMetaBox(grid, "REFERÊNCIA", refStr);

        metaCell.addElement(grid);
        table.addCell(metaCell);
        return table;
    }

    private static void addMetaBox(PdfPTable grid, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingBottom(6f);
        cell.addElement(new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, PdfTheme.MUTED)));
        cell.addElement(new Paragraph(value, PdfTheme.bodyFont()));
        grid.addCell(cell);
    }



    private static BigDecimal calculateTotalDiscount(Invoice invoice) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (invoice.getLines() != null) {
            for (InvoiceLine line : invoice.getLines()) {
                if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0
                        && line.getUnitPrice() != null && line.getQuantity() != null) {
                    BigDecimal lineGross = line.getUnitPrice().multiply(line.getQuantity());
                    BigDecimal disc = lineGross.multiply(line.getDiscountPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                    totalDiscount = totalDiscount.add(disc);
                }
            }
        }
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    private static PdfPTable buildPaymentAndNotesTable(Invoice invoice) {
        PdfPTable table = new PdfPTable(new float[]{50f, 50f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(8f);

        // Dados para Pagamento
        PdfPCell payCell = new PdfPCell();
        payCell.setBorder(PdfPCell.NO_BORDER);
        payCell.setPaddingRight(10f);

        Paragraph payTitle = new Paragraph("DADOS PARA PAGAMENTO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED));
        payTitle.setSpacingAfter(3f);
        payCell.addElement(payTitle);

        payCell.addElement(new Paragraph("Banco: Millennium BIM / BCI", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PdfTheme.TEXT)));
        payCell.addElement(new Paragraph("Conta / NIB: 0001 0000 0123 4567 8901 2", PdfTheme.bodyFont()));
        payCell.addElement(new Paragraph("BIC/SWIFT: BIMXMZMA", PdfTheme.bodyFont()));
        payCell.addElement(new Paragraph("Referência: " + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "—"), PdfTheme.bodyFont()));

        table.addCell(payCell);

        // Observações
        PdfPCell obsCell = new PdfPCell();
        obsCell.setBorder(PdfPCell.NO_BORDER);
        obsCell.setPaddingLeft(10f);

        Paragraph obsTitle = new Paragraph("OBSERVAÇÕES", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED));
        obsTitle.setSpacingAfter(3f);
        obsCell.addElement(obsTitle);

        StringBuilder obsText = new StringBuilder("Prazo de entrega e garantia conforme condições comerciais contratadas.");
        if (invoice.getCancellationReason() != null && !invoice.getCancellationReason().isBlank()) {
            obsText.append("\nMotivo de Anulação: ").append(invoice.getCancellationReason().trim());
        }
        if (invoice.getContingencyReference() != null && !invoice.getContingencyReference().isBlank()) {
            obsText.append("\nEmitido em regime de contingência: ").append(invoice.getContingencyReference().trim());
        }
        if (invoice.getStatus() != null) {
            obsText.append("\nEstado: ").append(invoice.getStatus());
        }
        obsCell.addElement(new Paragraph(obsText.toString(), PdfTheme.smallFont()));

        table.addCell(obsCell);
        return table;
    }

    private static Paragraph buildLegalDisclaimer() {
        Paragraph disclaimer = new Paragraph(
                "Os bens/serviços foram colocados à disposição do adquirente à data da emissão desta fatura. "
                        + "Em caso de atraso no pagamento poderão ser cobrados juros de mora à taxa legal em vigor. "
                        + "Obrigado pela sua preferência.",
                FontFactory.getFont(FontFactory.HELVETICA, 7.5f, PdfTheme.MUTED)
        );
        disclaimer.setSpacingBefore(6f);
        disclaimer.setSpacingAfter(6f);
        return disclaimer;
    }

    private static String formatRate(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) return "0%";
        BigDecimal percent = rate.multiply(BigDecimal.valueOf(100));
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    private static final class InvoiceFooterEvent extends PdfPageEventHelper {
        private final Company company;
        private final List<String> copies;

        InvoiceFooterEvent(Company company, List<String> copies) {
            this.company = company;
            this.copies = copies;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            int pageNum = writer.getPageNumber();
            String copyLabel = (pageNum <= copies.size()) ? copies.get(pageNum - 1) : ("Página " + pageNum);

            String simpleCopy = copyLabel.contains("ORIGINAL") ? "Original"
                    : (copyLabel.contains("DUPLICADO") ? "Duplicado"
                    : (copyLabel.contains("TRIPLICADO") ? "Triplicado"
                    : (copyLabel.contains("QUADRUPLICADO") ? "Quadruplicado" : copyLabel)));

            PdfContentByte cb = writer.getDirectContent();
            float left = document.left();
            float right = document.right();
            float bottom = document.bottom() - 12f;

            // Linha divisória
            cb.setColorStroke(PdfTheme.BORDER);
            cb.setLineWidth(0.5f);
            cb.moveTo(left, bottom + 16f);
            cb.lineTo(right, bottom + 16f);
            cb.stroke();

            // Texto legal / certificação à esquerda
            String line1 = "Processado por programa certificado n.º 001/DGI/MC · Multicore ERP";
            String compName = company != null && company.getName() != null ? company.getName() : "Multicore ERP";
            String taxId = company != null && company.getTaxId() != null ? company.getTaxId() : "";
            String addr = company != null && company.getAddress() != null ? company.getAddress() : "Maputo, Moçambique";
            String line2 = compName + " · NUIT " + taxId + " · " + addr;

            ColumnText.showTextAligned(
                    cb, Element.ALIGN_LEFT,
                    new Phrase(line1, FontFactory.getFont(FontFactory.HELVETICA, 7.2f, PdfTheme.MUTED)),
                    left, bottom + 6f, 0
            );
            ColumnText.showTextAligned(
                    cb, Element.ALIGN_LEFT,
                    new Phrase(line2, FontFactory.getFont(FontFactory.HELVETICA, 7.2f, PdfTheme.MUTED)),
                    left, bottom - 3f, 0
            );

            // Cópia e Página à direita
            String pageStamp = simpleCopy + " · Página " + pageNum;
            ColumnText.showTextAligned(
                    cb, Element.ALIGN_RIGHT,
                    new Phrase(pageStamp, FontFactory.getFont(FontFactory.HELVETICA, 7.5f, PdfTheme.MUTED)),
                    right, bottom + 6f, 0
            );
        }
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

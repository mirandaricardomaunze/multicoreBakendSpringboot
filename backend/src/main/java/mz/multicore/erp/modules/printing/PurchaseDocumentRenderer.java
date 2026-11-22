package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.purchases.dto.PurchaseDocumentPrintDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * A4 layout shared by the purchase-order and purchase PDFs (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3.1):
 * every amount is in the document currency with its code; a foreign-currency document also shows the stored rate
 * and the total in the base currency. Not a fiscal document, so no reprint control.
 */
@Component
public class PurchaseDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public byte[] render(Company company, String title, PurchaseDocumentPrintDTO document) {
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, title, document.documentNumber()));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(detailsBlock(document));
            doc.add(linesTable(document));
            doc.add(totalsBlock(document));
        });
    }

    private PdfPTable detailsBlock(PurchaseDocumentPrintDTO d) {
        PdfPTable table = new PdfPTable(new float[]{55f, 45f});
        table.setWidthPercentage(100);
        PdfPCell supplier = boxCell();
        supplier.addElement(new Phrase("Fornecedor: " + orDash(d.supplierName()), PdfTheme.boldFont()));
        supplier.addElement(new Phrase("\nNUIT: " + orDash(d.supplierTaxId()), PdfTheme.bodyFont()));
        supplier.addElement(new Phrase("\nArmazém: " + orDash(d.warehouseName()), PdfTheme.bodyFont()));
        PdfPCell document = boxCell();
        document.addElement(new Phrase("Data: " + (d.documentDate() != null ? d.documentDate().format(DATE_FMT) : "—"),
                PdfTheme.bodyFont()));
        document.addElement(new Phrase("\n" + currencyLabel(d), PdfTheme.bodyFont()));
        table.addCell(supplier);
        table.addCell(document);
        return table;
    }

    /** "Moeda: USD · Câmbio: 63,9055" em moeda estrangeira; só "Moeda: MZN" na moeda base. */
    static String currencyLabel(PurchaseDocumentPrintDTO d) {
        String label = "Moeda: " + d.currency();
        return d.foreignCurrency() ? label + " · Câmbio: " + DisplayNumbers.rate(d.exchangeRate()) : label;
    }

    private PdfPTable linesTable(PurchaseDocumentPrintDTO d) {
        PdfPTable table = new PdfPTable(new float[]{40f, 12f, 18f, 10f, 20f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(6f);
        for (String header : new String[]{"Artigo", "Qtd.", "Preço unit.", "IVA %", "Total"}) {
            table.addCell(cell(header, PdfTheme.tableHeaderFont(), Element.ALIGN_CENTER, true));
        }
        table.setHeaderRows(1);
        for (PurchaseDocumentPrintDTO.Line line : d.lines()) {
            table.addCell(cell(orDash(line.description()), PdfTheme.tableBodyFont(), Element.ALIGN_LEFT, false));
            table.addCell(cell(MoneyFormat.formatQty(line.quantity()), PdfTheme.tableBodyFont(), Element.ALIGN_RIGHT, false));
            table.addCell(cell(MoneyFormat.format(line.unitPrice(), d.currency()), PdfTheme.tableBodyFont(),
                    Element.ALIGN_RIGHT, false));
            table.addCell(cell(MoneyFormat.formatQty(percent(line.taxRate())) + "%", PdfTheme.tableBodyFont(),
                    Element.ALIGN_RIGHT, false));
            table.addCell(cell(MoneyFormat.format(line.lineTotal(), d.currency()), PdfTheme.tableBodyFont(),
                    Element.ALIGN_RIGHT, false));
        }
        return table;
    }

    private PdfPTable totalsBlock(PurchaseDocumentPrintDTO d) {
        PdfPTable table = new PdfPTable(new float[]{60f, 22f, 18f});
        table.setWidthPercentage(100);
        totalRow(table, "Total sem IVA", MoneyFormat.format(d.subtotal(), d.currency()));
        totalRow(table, "IVA", MoneyFormat.format(d.taxAmount(), d.currency()));
        totalRow(table, "Total", MoneyFormat.format(d.totalAmount(), d.currency()));
        if (d.foreignCurrency()) {
            totalRow(table, "Total em moeda base", MoneyFormat.format(d.baseTotalAmount(), d.baseCurrency()));
        }
        return table;
    }

    private void totalRow(PdfPTable table, String label, String value) {
        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        table.addCell(empty);
        table.addCell(cell(label, PdfTheme.boldFont(), Element.ALIGN_LEFT, false));
        table.addCell(cell(value, PdfTheme.boldFont(), Element.ALIGN_RIGHT, false));
    }

    private PdfPCell boxCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(8f);
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        return cell;
    }

    private PdfPCell cell(String text, com.lowagie.text.Font font, int align, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (header) cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        return cell;
    }

    private static BigDecimal percent(BigDecimal rate) {
        return rate == null ? BigDecimal.ZERO : rate.multiply(HUNDRED);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}

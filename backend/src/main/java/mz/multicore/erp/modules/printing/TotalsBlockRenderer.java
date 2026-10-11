package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.pricing.LineCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Right-aligned totals block and optional tax breakdown table used by invoice / order / credit note PDFs.
 * Follows the corporate standard of enterprise ERPs (SAP, Primavera, Sage) with explicit VAT discrimination.
 */
public final class TotalsBlockRenderer {

    public record TaxBreakdown(
            String taxRateLabel,
            BigDecimal ratePercentage,
            BigDecimal taxableAmount,
            BigDecimal taxAmount
    ) {}

    private TotalsBlockRenderer() {}

    public static PdfPTable build(BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
        return build(subtotal, tax, total, null);
    }

    public static PdfPTable build(BigDecimal subtotal, BigDecimal tax, BigDecimal total, List<TaxBreakdown> breakdowns) {
        PdfPTable wrapper = new PdfPTable(new float[]{55f, 45f});
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingBefore(6f);
        wrapper.setSpacingAfter(6f);

        if (breakdowns != null && !breakdowns.isEmpty()) {
            PdfPCell taxCell = new PdfPCell(buildTaxBreakdownTable(breakdowns));
            taxCell.setBorder(PdfPCell.NO_BORDER);
            taxCell.setPaddingRight(14f);
            wrapper.addCell(taxCell);
        } else {
            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setBorder(PdfPCell.NO_BORDER);
            wrapper.addCell(empty);
        }

        PdfPTable inner = new PdfPTable(new float[]{55f, 45f});
        inner.setWidthPercentage(100);
        line(inner, "Subtotal Ilíquido", MoneyFormat.format(subtotal), false);
        line(inner, "Total Imposto (IVA)", MoneyFormat.format(tax), false);
        line(inner, "TOTAL A PAGAR", MoneyFormat.format(total), true);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private static PdfPTable buildTaxBreakdownTable(List<TaxBreakdown> breakdowns) {
        PdfPTable t = new PdfPTable(new float[]{42f, 30f, 28f});
        t.setWidthPercentage(100);

        headerCell(t, "Taxa / Motivo", Element.ALIGN_LEFT);
        headerCell(t, "Incidência", Element.ALIGN_RIGHT);
        headerCell(t, "Valor IVA", Element.ALIGN_RIGHT);

        for (TaxBreakdown b : breakdowns) {
            String label = b.taxRateLabel() != null && !b.taxRateLabel().isBlank()
                    ? b.taxRateLabel()
                    : (b.ratePercentage() != null && b.ratePercentage().signum() > 0
                    ? String.format("IVA %.1f%%", b.ratePercentage().doubleValue())
                    : "IVA Isento");

            dataCell(t, label, Element.ALIGN_LEFT, false);
            dataCell(t, MoneyFormat.format(b.taxableAmount()), Element.ALIGN_RIGHT, false);
            dataCell(t, MoneyFormat.format(b.taxAmount()), Element.ALIGN_RIGHT, false);
        }

        return t;
    }

    private static void headerCell(PdfPTable t, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3.5f);
        t.addCell(cell);
    }

    private static void dataCell(PdfPTable t, String text, int align, boolean emphasised) {
        PdfPCell cell = new PdfPCell(new Phrase(text, emphasised ? PdfTheme.boldFont() : PdfTheme.smallFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3f);
        t.addCell(cell);
    }

    private static void line(PdfPTable t, String label, String value, boolean emphasised) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(value, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        l.setBorder(PdfPCell.NO_BORDER);
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3.5f);
        v.setPadding(3.5f);
        if (emphasised) {
            l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
            v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        t.addCell(l);
        t.addCell(v);
    }

    /**
     * Calcula o discriminativo agregado de IVA por taxa a partir das linhas de documento.
     */
    public static List<TaxBreakdown> calculateBreakdowns(List<LineItemsTableRenderer.Row> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        Map<BigDecimal, TaxAccumulator> map = new LinkedHashMap<>();
        for (LineItemsTableRenderer.Row row : rows) {
            BigDecimal rate = row.taxRate() != null ? row.taxRate() : BigDecimal.ZERO;
            TaxAccumulator acc = map.computeIfAbsent(rate, r -> new TaxAccumulator(r));

            BigDecimal qty = row.quantity() != null ? row.quantity() : BigDecimal.ZERO;
            BigDecimal price = row.unitPrice() != null ? row.unitPrice() : BigDecimal.ZERO;
            BigDecimal discount = row.discountPercentage() != null ? row.discountPercentage() : BigDecimal.ZERO;

            LineCalculator.LineAmounts amounts = LineCalculator.compute(price, qty, discount, rate);

            acc.taxable = acc.taxable.add(amounts.net());
            acc.tax = acc.tax.add(amounts.tax());
        }

        List<TaxBreakdown> result = new ArrayList<>();
        for (TaxAccumulator acc : map.values()) {
            String label = acc.rate.signum() == 0
                    ? "Isento (Art. 9/10)"
                    : String.format(Locale.forLanguageTag("pt-MZ"), "IVA %.1f%%", acc.rate.multiply(BigDecimal.valueOf(100)).doubleValue());

            result.add(new TaxBreakdown(
                    label,
                    acc.rate.multiply(BigDecimal.valueOf(100)),
                    acc.taxable.setScale(2, RoundingMode.HALF_UP),
                    acc.tax.setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return result;
    }

    private static final class TaxAccumulator {
        final BigDecimal rate;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;

        TaxAccumulator(BigDecimal rate) {
            this.rate = rate;
        }
    }
}

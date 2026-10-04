package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineItemsTableRendererTest {

    @Test
    void subtotal_isNetBeforeTax_withDiscount() {
        // 2 × 100 = 200; desconto 10% → 20; líquido = 180 (antes de IVA, IVA não conta no subtotal).
        LineItemsTableRenderer.Row row = new LineItemsTableRenderer.Row(
                "560000000001", "REF-1", "Iogurte", LocalDate.of(2026, 12, 31),
                new BigDecimal("2"), 12, 6, new BigDecimal("100"), new BigDecimal("0.16"),
                new BigDecimal("10.00"), new BigDecimal("208.80"));

        assertEquals(0, new BigDecimal("180.00").compareTo(LineItemsTableRenderer.subtotal(row)));
    }

    @Test
    void expiry_isDashWhenNull() {
        assertEquals("—", LineItemsTableRenderer.formatExpiry(null));
        assertEquals("31/12/2026", LineItemsTableRenderer.formatExpiry(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void build_hasElevenCanonicalColumnsWithAbbreviatedHeaders() {
        LineItemsTableRenderer.Row row = new LineItemsTableRenderer.Row(
                "560000000001", "REF-1", "Iogurte", LocalDate.of(2026, 12, 31),
                BigDecimal.ONE, 12, 6, new BigDecimal("100"), new BigDecimal("0.16"),
                BigDecimal.ZERO, new BigDecimal("116.00"));

        PdfPTable table = LineItemsTableRenderer.build(List.of(row));

        assertEquals(11, table.getNumberOfColumns());
        assertEquals(2, table.getRows().size()); // 1 cabeçalho + 1 linha
        assertEquals("Cód.", table.getRow(0).getCells()[0].getPhrase().getContent());
        assertEquals("Ref.", table.getRow(0).getCells()[1].getPhrase().getContent());
        assertEquals("Desc.", table.getRow(0).getCells()[2].getPhrase().getContent());
        assertEquals("Val.", table.getRow(0).getCells()[3].getPhrase().getContent());
        assertEquals("Qtd.", table.getRow(0).getCells()[4].getPhrase().getContent());
        assertEquals("Emb.", table.getRow(0).getCells()[5].getPhrase().getContent());
        assertEquals("Cx.", table.getRow(0).getCells()[6].getPhrase().getContent());
        assertEquals("% Cx.", table.getRow(0).getCells()[7].getPhrase().getContent());
        assertEquals("P. Unit.", table.getRow(0).getCells()[8].getPhrase().getContent());
        assertEquals("IVA", table.getRow(0).getCells()[9].getPhrase().getContent());
        assertEquals("Subt.", table.getRow(0).getCells()[10].getPhrase().getContent());
    }

    // DC-06
    @Test
    void build_withConfig_omitsHiddenColumns() {
        LineItemsTableRenderer.Row row = new LineItemsTableRenderer.Row(
                "560000000001", "REF-1", "Iogurte", LocalDate.of(2026, 12, 31),
                BigDecimal.ONE, 12, 6, new BigDecimal("100"), new BigDecimal("0.16"),
                BigDecimal.ZERO, new BigDecimal("116.00"));

        // barcode=false, expiry=false → restam 9 colunas.
        DocumentColumnsDTO cols = new DocumentColumnsDTO(false, true, true, false, true, true, true,
                true, true, true, true, null);
        PdfPTable table = LineItemsTableRenderer.build(List.of(row), cols);
        assertEquals(9, table.getNumberOfColumns());

        // build(rows) mantém as 11 canónicas.
        assertEquals(11, LineItemsTableRenderer.build(List.of(row)).getNumberOfColumns());
    }

    @Test
    void packagingColumns_explainEquivalentPackagesAndBoxPercentage() {
        LineItemsTableRenderer.Row row = new LineItemsTableRenderer.Row(
                "560000000001", "REF-1", "Iogurte", null,
                new BigDecimal("18"), 12, 6, new BigDecimal("100"), new BigDecimal("0.16"),
                BigDecimal.ZERO, new BigDecimal("2088.00"));

        assertEquals("3", LineItemsTableRenderer.formatPackages(row));
        assertEquals("0.25", LineItemsTableRenderer.formatBoxes(row));
        assertEquals("25%", LineItemsTableRenderer.formatBoxPercentage(row));
        assertEquals(0, new BigDecimal("1800.00").compareTo(LineItemsTableRenderer.subtotal(row)));

        LineItemsTableRenderer.Row moreThanOneBox = new LineItemsTableRenderer.Row(
                "560000000001", "REF-1", "Iogurte", null,
                new BigDecimal("90"), 12, 6, new BigDecimal("100"), new BigDecimal("0.16"),
                BigDecimal.ZERO, new BigDecimal("10440.00"));
        assertEquals("15", LineItemsTableRenderer.formatPackages(moreThanOneBox));
        assertEquals("1.25", LineItemsTableRenderer.formatBoxes(moreThanOneBox));
        assertEquals("125%", LineItemsTableRenderer.formatBoxPercentage(moreThanOneBox));
    }
}

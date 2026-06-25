package com.phcpro.modules.printing;

import com.lowagie.text.pdf.PdfPTable;
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
                new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("0.16"),
                new BigDecimal("10.00"), new BigDecimal("208.80"));

        assertEquals(0, new BigDecimal("180.00").compareTo(LineItemsTableRenderer.subtotal(row)));
    }

    @Test
    void expiry_isDashWhenNull() {
        assertEquals("—", LineItemsTableRenderer.formatExpiry(null));
        assertEquals("31/12/2026", LineItemsTableRenderer.formatExpiry(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void build_hasEightCanonicalColumns() {
        LineItemsTableRenderer.Row row = new LineItemsTableRenderer.Row(
                "560000000001", "REF-1", "Iogurte", LocalDate.of(2026, 12, 31),
                BigDecimal.ONE, new BigDecimal("100"), new BigDecimal("0.16"),
                BigDecimal.ZERO, new BigDecimal("116.00"));

        PdfPTable table = LineItemsTableRenderer.build(List.of(row));

        assertEquals(8, table.getNumberOfColumns());
        assertEquals(2, table.getRows().size()); // 1 cabeçalho + 1 linha
    }
}

package com.phcpro.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosLayoutTest {

    @Test
    void narrowCartKeepsEveryColumnVisibleWithoutHorizontalScroll() {
        assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS,
                PosLayout.cartAutoResizeMode(PosLayout.CART_COMFORTABLE_WIDTH - 1));
    }

    @Test
    void comfortableCartFillsAvailableViewport() {
        assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS,
                PosLayout.cartAutoResizeMode(PosLayout.CART_COMFORTABLE_WIDTH));
    }

    @Test
    void operationalCartUsesSixCompactColumns() {
        JTable table = new JTable(new DefaultTableModel(
                new String[]{"Artigo", "Qtd", "Preço", "Desc.", "IVA", "Total"}, 0));
        PosLayout.configureOperationalCartColumns(table);
        assertEquals(6, table.getColumnCount());
        assertEquals(250, table.getColumnModel().getColumn(0).getPreferredWidth());
        assertEquals(55, table.getColumnModel().getColumn(1).getPreferredWidth());
        assertEquals(110, table.getColumnModel().getColumn(5).getPreferredWidth());
    }

    @Test
    void verticalRhythmPrioritizesTheCartViewport() {
        assertEquals(14, PosLayout.ROOT_VERTICAL_MARGIN);
        assertEquals(6, PosLayout.SECTION_VERTICAL_GAP);
        assertEquals(8, PosLayout.CARD_VERTICAL_GAP);
    }

    @Test
    void compactHeaderWeightsFillOneRow() {
        double total = java.util.Arrays.stream(PosLayout.HEADER_FIELD_WEIGHTS).sum();
        assertEquals(5, PosLayout.HEADER_FIELD_WEIGHTS.length);
        assertEquals(1.0, total, 0.0001);
    }
}

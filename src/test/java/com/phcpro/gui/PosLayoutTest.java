package com.phcpro.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosLayoutTest {

    @Test
    void narrowCartPreservesColumnWidthsAndUsesHorizontalScroll() {
        assertEquals(JTable.AUTO_RESIZE_OFF,
                PosLayout.cartAutoResizeMode(PosLayout.CART_COMFORTABLE_WIDTH - 1));
    }

    @Test
    void comfortableCartFillsAvailableViewport() {
        assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS,
                PosLayout.cartAutoResizeMode(PosLayout.CART_COMFORTABLE_WIDTH));
    }
}

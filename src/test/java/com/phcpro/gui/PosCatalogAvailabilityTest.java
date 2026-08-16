package com.phcpro.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosCatalogAvailabilityTest {

    @Test
    void allFilterIncludesAvailableAndOutOfStockProducts() {
        assertTrue(PosCatalogController.includeByAvailability(true, true));
        assertTrue(PosCatalogController.includeByAvailability(true, false));
    }

    @Test
    void availableFilterExcludesOutOfStockProducts() {
        assertTrue(PosCatalogController.includeByAvailability(false, true));
        assertFalse(PosCatalogController.includeByAvailability(false, false));
    }

    @Test
    void catalogUsesBoundedServerPages() {
        assertTrue(PosCatalogController.PAGE_SIZE > 0);
        assertTrue(PosCatalogController.PAGE_SIZE <= 50);
    }
}

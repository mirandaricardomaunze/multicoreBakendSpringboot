package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosCatalogAvailabilityTest {

    @Test
    void catalogCardsKeepCompactProfessionalDimensions() {
        assertTrue(PosCatalogController.CARD_IMAGE_WIDTH <= 100);
        assertTrue(PosCatalogController.CARD_IMAGE_HEIGHT <= 64);
        assertTrue(PosCatalogController.CARD_IMAGE_HEIGHT >= 56);
        assertTrue(PosCatalogController.CARD_PADDING >= 6);
        assertTrue(PosCatalogController.CARD_CONTENT_GAP >= 4);
    }

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

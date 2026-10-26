package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.pricing.TaxRates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de teste para validação da decomposição modular do POSPanel
 * (HARNESS-POS-PANEL-DECOMPOSITION-001 / SPEC-POS-PANEL-DECOMPOSITION-001).
 */
public class PosPanelDecompositionHarnessTest {

    @Test
    @DisplayName("DECOMP-01: Limite de Linhas do POSPanel (<= 450 linhas)")
    void testPosPanelLineCount() throws IOException {
        Path path = findGuiFile("POSPanel.java");
        assertTrue(Files.exists(path), "POSPanel.java deve existir");
        long lines = Files.lines(path).count();
        assertTrue(lines <= 450,
                String.format("POSPanel.java deve ter <= 450 linhas; actual: %d", lines));
    }

    @Test
    @DisplayName("DECOMP-02: Limite de Linhas dos Componentes Especializados Decompostos")
    void testSpecializedComponentsLineCount() throws IOException {
        Path checkoutCoordPath = findGuiFile("PosCheckoutCoordinator.java");
        assertTrue(Files.exists(checkoutCoordPath), "PosCheckoutCoordinator.java deve existir");
        long checkoutLines = Files.lines(checkoutCoordPath).count();
        assertTrue(checkoutLines <= 200,
                String.format("PosCheckoutCoordinator.java deve ter <= 200 linhas; actual: %d", checkoutLines));

        Path cartControllerPath = findGuiFile("PosCartController.java");
        assertTrue(Files.exists(cartControllerPath), "PosCartController.java deve existir");
        long cartLines = Files.lines(cartControllerPath).count();
        assertTrue(cartLines <= 200,
                String.format("PosCartController.java deve ter <= 200 linhas; actual: %d", cartLines));

        Path workspaceBuilderPath = findGuiFile("PosWorkspaceBuilder.java");
        assertTrue(Files.exists(workspaceBuilderPath), "PosWorkspaceBuilder.java deve existir");
        long wsLines = Files.lines(workspaceBuilderPath).count();
        assertTrue(wsLines <= 450,
                String.format("PosWorkspaceBuilder.java deve ter <= 450 linhas; actual: %d", wsLines));
    }

    @Test
    @DisplayName("DECOMP-03: Integridade de Métodos de Atalhos e Teclado no POSPanel")
    void testExposedOperationalMethods() {
        assertDoesNotThrow(() -> {
            Method clearCart = POSPanel.class.getDeclaredMethod("clearCart");
            assertNotNull(clearCart, "clearCart() deve existir em POSPanel");

            Method isHistoryView = POSPanel.class.getDeclaredMethod("isHistoryView");
            assertNotNull(isHistoryView, "isHistoryView() deve existir em POSPanel");

            Method editSelectedCartQuantity = POSPanel.class.getDeclaredMethod("editSelectedCartQuantity");
            assertNotNull(editSelectedCartQuantity, "editSelectedCartQuantity() deve existir em POSPanel");

            Method removeFromCart = POSPanel.class.getDeclaredMethod("removeFromCart");
            assertNotNull(removeFromCart, "removeFromCart() deve existir em POSPanel");

            Method showReturnDialog = POSPanel.class.getDeclaredMethod("showReturnDialog");
            assertNotNull(showReturnDialog, "showReturnDialog() deve existir em POSPanel");

            Method runCheckout = POSPanel.class.getDeclaredMethod("runCheckout");
            assertNotNull(runCheckout, "runCheckout() deve existir em POSPanel");
        });
    }

    @Test
    @DisplayName("DECOMP-04: Cálculo de Imposto Efetivo e Suporte de Fallback")
    void testEffectiveTaxRateFallback() {
        assertEquals(TaxRates.STANDARD_VAT, POSPanel.effectiveTaxRate(null));
        assertEquals(new BigDecimal("0.05"), POSPanel.effectiveTaxRate(new BigDecimal("0.05")));
        assertEquals(TaxRates.STANDARD_VAT, PosCartController.effectiveTaxRate(null));
    }

    private Path findGuiFile(String filename) {
        Path path = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", filename);
        if (!Files.exists(path)) {
            path = Path.of("desktop", "src", "main", "java", "mz", "multicore", "erp", "gui", filename);
        }
        return path;
    }
}

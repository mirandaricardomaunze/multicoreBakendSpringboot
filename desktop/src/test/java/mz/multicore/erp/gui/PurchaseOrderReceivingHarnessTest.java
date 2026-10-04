package mz.multicore.erp.gui;

import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderLineDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de validação do diálogo de recepção e conferência física de compras.
 */
class PurchaseOrderReceivingHarnessTest {

    private static Path resolveSourcePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", "desktop", relativePath);
    }

    @Test
    @DisplayName("POR-01: PurchaseOrderReceivingDialog possui colunas canónicas de conferência com 2 casas decimais")
    void testPurchaseOrderReceivingDialogStructure() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/PurchaseOrderReceivingDialog.java");
        assertTrue(Files.exists(path), "PurchaseOrderReceivingDialog.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("COL_TO_RECEIVE"), "Deve conter coluna A Receber");
        assertTrue(content.contains("COL_DAMAGED"), "Deve conter coluna Danificado");
        assertTrue(content.contains("COL_MISSING"), "Deve conter coluna Falta Definitiva");
        assertTrue(content.contains("TableCellRenderers.quantity()"), "Deve usar TableCellRenderers.quantity()");
        assertTrue(content.contains("ReceivePurchaseOrderRequest"), "Deve construir payload ReceivePurchaseOrderRequest");
        assertFalse(content.contains("⭐") || content.contains("✅") || content.contains("❌"), "Sem emojis");
    }

    @Test
    @DisplayName("POR-02: PurchaseOrdersPanel utiliza PurchaseOrderReceivingDialog na recepção parcial")
    void testPurchaseOrdersPanelIntegration() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java");
        assertTrue(Files.exists(path), "PurchaseOrdersPanel.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("PurchaseOrderReceivingDialog.show"), "Deve chamar PurchaseOrderReceivingDialog.show");
    }
}

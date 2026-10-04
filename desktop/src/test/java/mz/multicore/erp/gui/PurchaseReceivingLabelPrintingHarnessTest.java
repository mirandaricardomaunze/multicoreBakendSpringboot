package mz.multicore.erp.gui;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderLineDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de validação da emissão directa de etiquetas na recepção de mercadorias.
 */
class PurchaseReceivingLabelPrintingHarnessTest {

    private static Path resolveSourcePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", "desktop", relativePath);
    }

    @Test
    @DisplayName("PRL-01: extractLabelProducts extrai produtos apenas com quantidade positiva a receber")
    void testExtractLabelProducts() {
        List<PurchaseOrderLineDTO> lines = List.of(
                new PurchaseOrderLineDTO(1L, 101L, "Teclado Mecânico USB", "TEC-01",
                        new BigDecimal("10.00"), new BigDecimal("0.00"), new BigDecimal("1500.00"),
                        new BigDecimal("16.00"), new BigDecimal("15000.00"), "LOTE-A", null, null),
                new PurchaseOrderLineDTO(2L, 102L, "Rato Sem Fios 2.4GHz", "RAT-02",
                        new BigDecimal("5.00"), new BigDecimal("0.00"), new BigDecimal("600.00"),
                        new BigDecimal("16.00"), new BigDecimal("3000.00"), "LOTE-B", null, null),
                new PurchaseOrderLineDTO(3L, 103L, "Monitor 24 Pol IPS", "MON-03",
                        new BigDecimal("2.00"), new BigDecimal("0.00"), new BigDecimal("9500.00"),
                        new BigDecimal("16.00"), new BigDecimal("19000.00"), "LOTE-C", null, null)
        );

        String[] cols = {
                "Produto", "Encomendado", "Já Recebido", "Pendente",
                "A Receber (Boas)", "Danificado", "Falta Definitiva", "Notas / Divergência"
        };
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        model.addRow(new Object[]{"Teclado Mecânico USB", new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("10.00"), new BigDecimal("3.00"), BigDecimal.ZERO, BigDecimal.ZERO, ""});
        model.addRow(new Object[]{"Rato Sem Fios 2.4GHz", new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, ""});
        model.addRow(new Object[]{"Monitor 24 Pol IPS", new BigDecimal("2.00"), BigDecimal.ZERO, new BigDecimal("2.00"), new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO, ""});

        List<ProductDTO> labels = PurchaseOrderReceivingDialog.extractLabelProducts(lines, model, false);
        assertEquals(2, labels.size(), "Deve conter apenas os 2 artigos com quantidade a receber positiva");

        ProductDTO first = labels.get(0);
        assertEquals(101L, first.id());
        assertEquals("TEC-01", first.sku());
        assertEquals("Teclado Mecânico USB", first.name());
        assertEquals(new BigDecimal("1500.00"), first.unitPrice());

        ProductDTO second = labels.get(1);
        assertEquals(103L, second.id());
        assertEquals("MON-03", second.sku());
        assertEquals("Monitor 24 Pol IPS", second.name());
        assertEquals(new BigDecimal("9500.00"), second.unitPrice());

        // Test repeat by quantity
        List<ProductDTO> repeatedLabels = PurchaseOrderReceivingDialog.extractLabelProducts(lines, model, true);
        assertEquals(5, repeatedLabels.size(), "3 teclados + 2 monitores = 5 etiquetas geradas");
    }

    @Test
    @DisplayName("PRL-02: extractReceivedProducts em PurchaseOrdersPanel extrai artigos de encomenda recebida")
    void testExtractReceivedProductsFromOrder() {
        PurchaseOrderLineDTO l1 = new PurchaseOrderLineDTO(1L, 201L, "Cabo HDMI 2.0", "CAB-01",
                new BigDecimal("20.00"), new BigDecimal("20.00"), new BigDecimal("250.00"),
                new BigDecimal("16.00"), new BigDecimal("5000.00"), null, null, null);
        PurchaseOrderLineDTO l2 = new PurchaseOrderLineDTO(2L, 202L, "Adaptador Type-C", "ADP-02",
                new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("450.00"),
                new BigDecimal("16.00"), new BigDecimal("4500.00"), null, null, null);

        PurchaseOrderDTO order = new PurchaseOrderDTO(
                10L, "PO-2026-0010", 1L, "Fornecedor Tech Lda", 1L, 1L, LocalDate.now(),
                LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("9500.00"), new BigDecimal("9500.00"),
                "RECEIVED", "Entregue", List.of(l1, l2)
        );

        List<ProductDTO> labels = PurchaseOrdersPanel.extractReceivedProducts(order);
        assertNotNull(labels);
        assertFalse(labels.isEmpty());
        assertEquals("CAB-01", labels.get(0).sku());
        assertEquals("Cabo HDMI 2.0", labels.get(0).name());
    }

    @Test
    @DisplayName("PRL-03: Diálogo e painel integram botão e ação com ícone fas-barcode sem emojis")
    void testFileIntegrationsAndConventions() throws IOException {
        Path dialogPath = resolveSourcePath("src/main/java/mz/multicore/erp/gui/PurchaseOrderReceivingDialog.java");
        assertTrue(Files.exists(dialogPath), "PurchaseOrderReceivingDialog.java deve existir");
        String dialogContent = Files.readString(dialogPath);

        assertTrue(dialogContent.contains("ShelfLabelsDialog"), "Deve referenciar ShelfLabelsDialog");
        assertTrue(dialogContent.contains("fas-barcode"), "Deve utilizar o ícone fas-barcode");
        assertTrue(dialogContent.contains("extractLabelProducts"), "Deve conter método extractLabelProducts");
        assertFalse(dialogContent.contains("⭐") || dialogContent.contains("✅") || dialogContent.contains("❌"), "Sem emojis");

        Path panelPath = resolveSourcePath("src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java");
        assertTrue(Files.exists(panelPath), "PurchaseOrdersPanel.java deve existir");
        String panelContent = Files.readString(panelPath);

        assertTrue(panelContent.contains("Imprimir Etiquetas"), "Deve conter opção Imprimir Etiquetas");
        assertTrue(panelContent.contains("extractReceivedProducts"), "Deve conter extractReceivedProducts");
        assertFalse(panelContent.contains("⭐") || panelContent.contains("✅") || panelContent.contains("❌"), "Sem emojis");
    }
}

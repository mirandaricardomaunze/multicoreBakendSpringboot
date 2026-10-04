package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.pos.PosCartItem;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Harness de validação de UI para Conversão de Cotações e Importação no POS.
 * Conforme especificado em docs/COTACAO_CONVERSAO_HARNESS.md (UI-CONV-01 a UI-CONV-04).
 */
public class QuotationConversionUiHarnessTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    private static Path resolveSourcePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", "desktop", relativePath);
    }

    @Test
    @DisplayName("UI-CONV-01: QuotationsPanel possui ação 'Converter em Factura' com ícone FontAwesome e sem emojis")
    void testQuotationsPanelConversionAction() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java");
        assertTrue(Files.exists(path), "QuotationsPanel.java deve existir em " + path);

        String content = Files.readString(path);
        assertTrue(content.contains("Converter em Factura"), "QuotationsPanel deve conter a ação 'Converter em Factura'");
        assertTrue(content.contains("fas-file-invoice-dollar"), "Deve usar ícone vetorial FontAwesome fas-file-invoice-dollar");
        assertTrue(content.contains("convertToInvoice"), "Deve conter método handler convertToInvoice()");

        // Não deve conter emojis crus
        assertFalse(content.contains("⭐") || content.contains("✅") || content.contains("❌") || content.contains("💰"),
                "QuotationsPanel não deve conter emojis crus Unicode");
    }

    @Test
    @DisplayName("UI-CONV-02: Diálogo PosImportQuotationDialog é instanciável e segue padrões visuais")
    void testPosImportQuotationDialogInstantiable() {
        ComercialApiClient clientMock = mock(ComercialApiClient.class);
        assertDoesNotThrow(() -> {
            PosImportQuotationDialog dialog = new PosImportQuotationDialog(null, clientMock, 1L, quot -> {});
            assertNotNull(dialog);
            assertEquals("Importar Cotação no POS", dialog.getTitle());
            dialog.dispose();
        });
    }

    @Test
    @DisplayName("UI-CONV-03: PosCartItem suporta preço unitário cotado (customUnitPrice) com precedência sobre catálogo")
    void testPosCartItemCustomUnitPricePrecedence() {
        // Produto do catálogo a 1000 MT com 16% de IVA
        ProductDTO catalogProduct = new ProductDTO(
                10L, "SKU-001", "REF-001", "123456789", "Cimento Seco",
                new BigDecimal("1000.00"), new BigDecimal("700.00"), BigDecimal.ZERO,
                new BigDecimal("900.00"), new BigDecimal("10"), 1,
                "UN", true, 1L, "Construção", 1L, new BigDecimal("0.16"), "16%",
                "Cimento", null, null, null
        );

        // Item padrão sem preço especial (deve usar 1000 MT)
        PosCartItem normalItem = new PosCartItem(catalogProduct, new BigDecimal("2"), BigDecimal.ZERO, null, null);
        assertEquals(0, new BigDecimal("1000.00").compareTo(normalItem.getEffectiveUnitPrice()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(normalItem.getSubtotal()));
        assertEquals(0, new BigDecimal("320.00").compareTo(normalItem.getTax()));
        assertEquals(0, new BigDecimal("2320.00").compareTo(normalItem.getTotal()));

        // Item com preço cotado acordado a 850 MT
        BigDecimal quotedPrice = new BigDecimal("850.00");
        PosCartItem quotedItem = new PosCartItem(catalogProduct, new BigDecimal("2"), BigDecimal.ZERO, null, null, quotedPrice);
        assertEquals(0, quotedPrice.compareTo(quotedItem.getEffectiveUnitPrice()), "Preço cotado deve ter precedência");
        assertEquals(0, new BigDecimal("1700.00").compareTo(quotedItem.getSubtotal()));
        assertEquals(0, new BigDecimal("272.00").compareTo(quotedItem.getTax()));
        assertEquals(0, new BigDecimal("1972.00").compareTo(quotedItem.getTotal()));
    }

    @Test
    @DisplayName("UI-CONV-04: Limite estrito de < 1000 linhas em ficheiros Swing de cotação e POS")
    void testFileLinesStrictlyUnder1000() throws IOException {
        List<String> filesToCheck = List.of(
                "src/main/java/mz/multicore/erp/gui/POSPanel.java",
                "src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java",
                "src/main/java/mz/multicore/erp/gui/PosImportQuotationDialog.java",
                "src/main/java/mz/multicore/erp/gui/PosQuotationActions.java"
        );

        for (String file : filesToCheck) {
            Path path = resolveSourcePath(file);
            assertTrue(Files.exists(path), "Arquivo deve existir: " + file);
            long lineCount = Files.lines(path).count();
            assertTrue(lineCount < 1000,
                    "Arquivo " + path.getFileName() + " tem " + lineCount + " linhas, violando a regra de < 1000 linhas!");
        }
    }
}

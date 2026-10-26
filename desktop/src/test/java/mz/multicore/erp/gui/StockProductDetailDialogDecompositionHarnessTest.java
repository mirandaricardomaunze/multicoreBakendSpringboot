package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição de StockProductDetailDialog (Fase 83)")
class StockProductDetailDialogDecompositionHarnessTest {

    private static final Path SOURCE_DIR = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("Garante limites físicos estritos para StockProductDetailDialog e colaboradores")
    void testPhysicalLineLimits() throws IOException {
        Path dialogFile = SOURCE_DIR.resolve("StockProductDetailDialog.java");
        Path heroFile = SOURCE_DIR.resolve("StockProductDetailHeroBuilder.java");
        Path tabsFile = SOURCE_DIR.resolve("StockProductDetailTabsBuilder.java");

        assertTrue(Files.exists(dialogFile), "StockProductDetailDialog.java deve existir");
        assertTrue(Files.exists(heroFile), "StockProductDetailHeroBuilder.java deve existir");
        assertTrue(Files.exists(tabsFile), "StockProductDetailTabsBuilder.java deve existir");

        List<String> dialogLines = Files.readAllLines(dialogFile);
        List<String> heroLines = Files.readAllLines(heroFile);
        List<String> tabsLines = Files.readAllLines(tabsFile);

        assertTrue(dialogLines.size() <= 190, "StockProductDetailDialog deve ter <= 190 linhas (atual: " + dialogLines.size() + ")");
        assertTrue(heroLines.size() <= 140, "StockProductDetailHeroBuilder deve ter <= 140 linhas (atual: " + heroLines.size() + ")");
        assertTrue(tabsLines.size() <= 145, "StockProductDetailTabsBuilder deve ter <= 145 linhas (atual: " + tabsLines.size() + ")");
    }

    @Test
    @DisplayName("Proíbe o uso de JOptionPane nos componentes da ficha de artigo")
    void testNoJOptionPaneUsage() throws IOException {
        Path dialogFile = SOURCE_DIR.resolve("StockProductDetailDialog.java");
        Path heroFile = SOURCE_DIR.resolve("StockProductDetailHeroBuilder.java");
        Path tabsFile = SOURCE_DIR.resolve("StockProductDetailTabsBuilder.java");

        for (Path p : List.of(dialogFile, heroFile, tabsFile)) {
            String content = Files.readString(p);
            assertFalse(content.contains("JOptionPane"), "Ficheiro " + p.getFileName() + " não pode conter JOptionPane");
        }
    }

    @Test
    @DisplayName("Garante métodos públicos preservados no StockProductDetailDialog")
    void testPublicApiPreserved() {
        assertDoesNotThrow(() -> {
            StockProductDetailDialog.class.getConstructor(StockPanel.class, mz.multicore.erp.modules.comercial.dto.ProductDTO.class);
            StockProductDetailDialog.class.getMethod("showDialog");
            StockProductDetailDialog.class.getMethod("showFor", StockPanel.class, Long.class);
        });
    }

    @Test
    @DisplayName("Garante formatação monetária segura e padronizada em StockProductDetailHeroBuilder")
    void testMoneyFormatting() {
        assertEquals("0,00 MT", StockProductDetailHeroBuilder.formatMoney(null));
        String formatted = StockProductDetailHeroBuilder.formatMoney(new java.math.BigDecimal("1500.50"));
        assertTrue(formatted.contains("500,50"), "Formatação de moeda deve conter 500,50: " + formatted);
        assertTrue(formatted.endsWith("MT"), "Formatação de moeda deve terminar com MT: " + formatted);
    }
}

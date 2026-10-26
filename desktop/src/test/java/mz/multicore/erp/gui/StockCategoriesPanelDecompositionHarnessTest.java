package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-STOCK-CATEGORIES-PANEL-DECOMPOSITION-001: Decomposição do Painel de Categorias de Produto")
class StockCategoriesPanelDecompositionHarnessTest {

    @Test
    @DisplayName("SCP-01: StockCategoriesPanel.java reduzido para <= 100 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/StockCategoriesPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/StockCategoriesPanel.java");
        }
        assertTrue(Files.exists(path), "StockCategoriesPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 100, "StockCategoriesPanel.java deve possuir <= 100 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("SCP-02: Colaboradores desacoplados com limites rigorosos")
    void testCollaboratorsLineCount() throws IOException {
        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        Path formPath = baseDir.resolve("StockCategoryFormDialog.java");
        assertTrue(Files.exists(formPath), "StockCategoryFormDialog.java deve existir");
        List<String> formLines = Files.readAllLines(formPath);
        assertTrue(formLines.size() <= 95, "StockCategoryFormDialog.java deve possuir <= 95 linhas. Atual: " + formLines.size());

        Path coordPath = baseDir.resolve("StockCategoriesViewCoordinator.java");
        assertTrue(Files.exists(coordPath), "StockCategoriesViewCoordinator.java deve existir");
        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 130, "StockCategoriesViewCoordinator.java deve possuir <= 130 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("SCP-03: Erradicação absoluta de JOptionPane em StockCategoriesPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "StockCategoriesPanel.java",
                "StockCategoryFormDialog.java",
                "StockCategoriesViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> content = Files.readAllLines(filePath);
            for (int i = 0; i < content.size(); i++) {
                assertFalse(content.get(i).contains("JOptionPane"),
                        file + " contém JOptionPane na linha " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("SCP-04: Construtor package-private preservado")
    void testConstructorPreserved() throws NoSuchMethodException {
        assertNotNull(StockCategoriesPanel.class.getDeclaredConstructor(StockPanel.class));
    }

    @Test
    @DisplayName("SCP-05: Invariantes canónicas de layout e filtros preservadas")
    void testCanonicalInvariants() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/StockCategoriesPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/StockCategoriesPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop(\"Categorias de Produto\""),
                "Deve conter cabeçalho canónico tableCardTop(\"Categorias de Produto\"");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(owner.categoriesTable)"),
                "Deve calibrar colunas com ensureHeadersFit");
        assertTrue(content.contains("TableQuickPeekController.install(owner.categoriesTable"),
                "Deve instalar TableQuickPeekController");
        assertTrue(content.contains("TableFilter.bar("),
                "Deve conter barra horizontal TableFilter.bar");
        assertFalse(content.contains("new JPanel(new GridBagLayout())"),
                "Proibido usar GridBagLayout em filtros");
    }
}

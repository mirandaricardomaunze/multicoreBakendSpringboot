package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: StockBatchesPanel")
class StockBatchesPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("BATCH-DEC-01: StockBatchesPanel.java deve possuir <= 165 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("StockBatchesPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165,
                () -> "StockBatchesPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("BATCH-DEC-02: StockBatchesViewCoordinator.java deve possuir <= 140 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path coord = ROOT.resolve("StockBatchesViewCoordinator.java");
        List<String> lines = Files.readAllLines(coord);
        assertTrue(lines.size() <= 140,
                () -> "StockBatchesViewCoordinator.java deve possuir <= 140 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("BATCH-DEC-03: Contratos canónicos literais preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("StockBatchesPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Lotes & Validades\""),
                "Falta UIHelper.tableCardTop com título Lotes & Validades");
        assertTrue(content.contains("TableFilter.bar("), "Falta TableFilter.bar(");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(owner.batchesTable)"),
                "Falta UIHelper.ensureHeadersFit(owner.batchesTable)");
        assertTrue(content.contains("ClientTablePagination.install(owner.batchesTable)"),
                "Falta ClientTablePagination.install(owner.batchesTable)");
        assertTrue(content.contains("TableQuickPeekController.install(owner.batchesTable"),
                "Falta TableQuickPeekController.install(owner.batchesTable");
    }

    @Test
    @DisplayName("BATCH-DEC-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("StockBatchesPanel.java", "StockBatchesViewCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("BATCH-DEC-05: Construtor package-private e métodos preservados")
    void testPublicApiPreserved() throws NoSuchMethodException {
        StockBatchesPanel.class.getDeclaredConstructor(StockPanel.class);
        StockBatchesPanel.class.getMethod("buildPanel");
        StockBatchesPanel.class.getMethod("refresh");
    }
}

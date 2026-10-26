package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: PurchaseReorderPanel")
class PurchaseReorderPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("PRP-01: PurchaseReorderPanel.java deve possuir <= 190 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("PurchaseReorderPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 190,
                () -> "PurchaseReorderPanel.java deve possuir <= 190 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("PRP-02: Colaboradores desacoplados devem respeitar os limites de linhas")
    void testCollaboratorsLineCount() throws IOException {
        Path coord = ROOT.resolve("PurchaseReorderOperationsCoordinator.java");
        List<String> coordLines = Files.readAllLines(coord);
        assertTrue(coordLines.size() <= 85,
                () -> "PurchaseReorderOperationsCoordinator.java deve possuir <= 85 linhas. Atual: " + coordLines.size());

        Path data = ROOT.resolve("PurchaseReorderDataCoordinator.java");
        List<String> dataLines = Files.readAllLines(data);
        assertTrue(dataLines.size() <= 115,
                () -> "PurchaseReorderDataCoordinator.java deve possuir <= 115 linhas. Atual: " + dataLines.size());
    }

    @Test
    @DisplayName("PRP-03: Contratos literais canónicos preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("PurchaseReorderPanel.java"));

        assertTrue(content.contains("KpiCard.createGrid(4)"), "Falta KpiCard.createGrid(4)");
        assertTrue(content.contains("KpiCard.createInteractiveCard("), "Falta KpiCard.createInteractiveCard(");
        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("UIHelper.tableCardTop("), "Falta UIHelper.tableCardTop(");
        assertTrue(content.contains("TableQuickPeekController.install(reorderTable"), "Falta TableQuickPeekController.install(reorderTable");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(reorderTable)"), "Falta UIHelper.ensureHeadersFit(reorderTable)");
    }

    @Test
    @DisplayName("PRP-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("PurchaseReorderPanel.java", "PurchaseReorderOperationsCoordinator.java",
                "PurchaseReorderDataCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }
}

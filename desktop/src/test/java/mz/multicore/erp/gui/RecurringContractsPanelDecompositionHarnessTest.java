package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.RecurringContractApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: RecurringContractsPanel")
class RecurringContractsPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("RCP-01: RecurringContractsPanel.java deve possuir <= 230 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("RecurringContractsPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 230,
                () -> "RecurringContractsPanel.java deve possuir <= 230 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("RCP-02: Colaboradores desacoplados devem respeitar os limites de linhas")
    void testCollaboratorsLineCount() throws IOException {
        Path formDialog = ROOT.resolve("RecurringContractFormDialog.java");
        List<String> formLines = Files.readAllLines(formDialog);
        assertTrue(formLines.size() <= 135,
                () -> "RecurringContractFormDialog.java deve possuir <= 135 linhas. Atual: " + formLines.size());

        Path coord = ROOT.resolve("RecurringContractsOperationsCoordinator.java");
        List<String> coordLines = Files.readAllLines(coord);
        assertTrue(coordLines.size() <= 95,
                () -> "RecurringContractsOperationsCoordinator.java deve possuir <= 95 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("RCP-03: Contratos literais canónicos preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("RecurringContractsPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Falta KpiCard.createGrid(4)");
        assertTrue(content.contains("UIHelper.tableCardTop("), "Falta UIHelper.tableCardTop(");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "Falta UIHelper.ensureHeadersFit(table)");
        assertTrue(content.contains("TableFilter.install("), "Falta TableFilter.install(");
    }

    @Test
    @DisplayName("RCP-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("RecurringContractsPanel.java", "RecurringContractFormDialog.java",
                "RecurringContractsOperationsCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("RCP-05: Construtor público e método onPanelSelected preservados")
    void testPublicApiPreserved() throws NoSuchMethodException {
        RecurringContractsPanel.class.getConstructor(RecurringContractApiClient.class);
        RecurringContractsPanel.class.getMethod("onPanelSelected");
    }
}

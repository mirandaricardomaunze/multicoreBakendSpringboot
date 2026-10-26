package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: ForensicAuditPanel")
class ForensicAuditPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("FAP-01: ForensicAuditPanel.java deve possuir <= 280 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("ForensicAuditPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 280,
                () -> "ForensicAuditPanel.java deve possuir <= 280 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("FAP-02: Colaboradores desacoplados devem respeitar os limites de linhas")
    void testCollaboratorsLineCount() throws IOException {
        Path dialog = ROOT.resolve("ForensicAnomalyDetailsDialog.java");
        List<String> dialogLines = Files.readAllLines(dialog);
        assertTrue(dialogLines.size() <= 75,
                () -> "ForensicAnomalyDetailsDialog.java deve possuir <= 75 linhas. Atual: " + dialogLines.size());

        Path coord = ROOT.resolve("ForensicAuditOperationsCoordinator.java");
        List<String> coordLines = Files.readAllLines(coord);
        assertTrue(coordLines.size() <= 115,
                () -> "ForensicAuditOperationsCoordinator.java deve possuir <= 115 linhas. Atual: " + coordLines.size());

        Path renderer = ROOT.resolve("ForensicCellRenderer.java");
        List<String> rendererLines = Files.readAllLines(renderer);
        assertTrue(rendererLines.size() <= 50,
                () -> "ForensicCellRenderer.java deve possuir <= 50 linhas. Atual: " + rendererLines.size());
    }

    @Test
    @DisplayName("FAP-03: Contratos literais canónicos preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("ForensicAuditPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Falta KpiCard.createGrid(4)");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "Falta UIHelper.ensureHeadersFit(table)");
        assertTrue(content.contains("TableQuickPeekController.install(table"), "Falta TableQuickPeekController.install(table");
        assertTrue(content.contains("TableFilter.toolbar("), "Falta TableFilter.toolbar(");
    }

    @Test
    @DisplayName("FAP-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("ForensicAuditPanel.java", "ForensicAnomalyDetailsDialog.java",
                "ForensicAuditOperationsCoordinator.java", "ForensicCellRenderer.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("FAP-05: Conformidade de ausência de new Color e sem palavras proibidas")
    void testNoNewColorAndNoForbiddenWords() throws IOException {
        List<String> lines = Files.readAllLines(ROOT.resolve("ForensicAuditPanel.java"));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            assertFalse(line.contains("new Color("),
                    "Linha " + (i + 1) + " não pode instanciar new Color(...). Use tokens do UIHelper: " + line);
            assertFalse(line.toLowerCase().contains("atualizar"),
                    "Linha " + (i + 1) + " não pode usar a palavra proibida 'atualizar'. Em Moçambique deve ser 'Actualizar': " + line);
        }
    }

    @Test
    @DisplayName("FAP-06: Construtor público e campos package-private preservados")
    void testPublicAndPackageApiPreserved() throws NoSuchMethodException, NoSuchFieldException {
        ForensicAuditPanel.class.getConstructor(ForensicAuditApiClient.class);

        ForensicAuditPanel.class.getDeclaredField("kpiTotalRisk");
        ForensicAuditPanel.class.getDeclaredField("kpiCritical");
        ForensicAuditPanel.class.getDeclaredField("kpiSuspicious");
        ForensicAuditPanel.class.getDeclaredField("kpiComplianceScore");
        ForensicAuditPanel.class.getDeclaredField("startDateField");
        ForensicAuditPanel.class.getDeclaredField("endDateField");
        ForensicAuditPanel.class.getDeclaredField("severityCombo");
        ForensicAuditPanel.class.getDeclaredField("categoryCombo");
        ForensicAuditPanel.class.getDeclaredField("operatorField");
        ForensicAuditPanel.class.getDeclaredField("table");
        ForensicAuditPanel.class.getDeclaredField("tableModel");
        ForensicAuditPanel.class.getDeclaredField("currentAnomalies");
        ForensicAuditPanel.class.getDeclaredMethod("renderSummary", mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO.class);
    }
}

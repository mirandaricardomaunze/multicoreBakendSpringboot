package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: ApprovalsPanel")
class ApprovalsPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("APP-01: ApprovalsPanel.java deve possuir <= 230 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("ApprovalsPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 230,
                () -> "ApprovalsPanel.java deve possuir <= 230 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("APP-02: Colaboradores desacoplados devem respeitar os limites de linhas")
    void testCollaboratorsLineCount() throws IOException {
        Path history = ROOT.resolve("ApprovalsHistoryView.java");
        List<String> historyLines = Files.readAllLines(history);
        assertTrue(historyLines.size() <= 95,
                () -> "ApprovalsHistoryView.java deve possuir <= 95 linhas. Atual: " + historyLines.size());

        Path coord = ROOT.resolve("ApprovalsOperationsCoordinator.java");
        List<String> coordLines = Files.readAllLines(coord);
        assertTrue(coordLines.size() <= 85,
                () -> "ApprovalsOperationsCoordinator.java deve possuir <= 85 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("APP-03: Contratos literais canónicos preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("ApprovalsPanel.java"));

        assertTrue(content.contains("UIHelper.ensureHeadersFit(pendingTable)"), "Falta UIHelper.ensureHeadersFit(pendingTable)");
        assertTrue(content.contains("TableQuickPeekController.install(pendingTable"), "Falta TableQuickPeekController.install(pendingTable");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Falta KpiCard.createGrid(4)");
        assertTrue(content.contains("UIHelper.tableCardTop("), "Falta UIHelper.tableCardTop(");
    }

    @Test
    @DisplayName("APP-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("ApprovalsPanel.java", "ApprovalsHistoryView.java", "ApprovalsOperationsCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("APP-05: Construtores sobrecarregados preservados")
    void testOverloadedConstructorsPreserved() throws NoSuchMethodException {
        ApprovalsPanel.class.getConstructor(ApprovalApiClient.class);
        ApprovalsPanel.class.getConstructor(ApprovalApiClient.class, StockTransferApiClient.class, ComercialApiClient.class);
        ApprovalsPanel.class.getConstructor(ApprovalApiClient.class, StockTransferApiClient.class, ComercialApiClient.class, PrintApiClient.class);
    }
}

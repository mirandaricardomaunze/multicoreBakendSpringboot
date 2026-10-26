package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.BankReconciliationApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de testes automatizados para conformidade com HARNESS-BANK-RECONCILIATION-PANEL-DECOMPOSITION-001.
 * Valida limites de linhas, desacoplamento em colaboradores, ausência de JOptionPane e instanciação de componentes.
 */
public class BankReconciliationPanelDecompositionHarnessTest {

    private static final Path GUI_PATH = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("TRECON-DEC-01: Todos os ficheiros desacoplados cumprem estritamente os seus tetos de linhas")
    void testFileLinesWithinLimits() throws IOException {
        assertLineCount(GUI_PATH.resolve("BankReconciliationPanel.java"), 450);
        assertLineCount(GUI_PATH.resolve("BankStatementImportDialog.java"), 190);
        assertLineCount(GUI_PATH.resolve("BankReconciliationMatchDialogs.java"), 190);
        assertLineCount(GUI_PATH.resolve("BankReconciliationOperationsCoordinator.java"), 160);
    }

    @Test
    @DisplayName("TRECON-DEC-02: Zero chamadas a JOptionPane em todos os ficheiros de reconciliação bancária")
    void testZeroJOptionPaneUsage() throws IOException {
        List<String> files = List.of(
                "BankReconciliationPanel.java",
                "BankStatementImportDialog.java",
                "BankReconciliationMatchDialogs.java",
                "BankReconciliationOperationsCoordinator.java"
        );

        for (String file : files) {
            Path path = GUI_PATH.resolve(file);
            assertTrue(Files.exists(path), file + " deve existir");
            List<String> lines = Files.readAllLines(path);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                assertFalse(line.contains("JOptionPane"),
                        String.format("Violação em %s na linha %d: proibido uso de JOptionPane.", file, i + 1));
            }
        }
    }

    @Test
    @DisplayName("TRECON-DEC-03: Instanciação limpa e verificação dos componentes e acções no BankReconciliationPanel")
    void testPanelInstantiationAndComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BankReconciliationApiClient apiClient = Mockito.mock(BankReconciliationApiClient.class);
            FinanceApiClient financeApiClient = Mockito.mock(FinanceApiClient.class);

            BankReconciliationPanel panel = new BankReconciliationPanel(apiClient, financeApiClient);
            assertNotNull(panel);
            assertNotNull(panel.itemsTable);
            assertNotNull(panel.accountCombo);
            assertNotNull(panel.statementCombo);
            assertNotNull(panel.operationsMenu);
            assertNotNull(panel.statusFilter);
            assertNotNull(panel.kpiBankBalance);
            assertNotNull(panel.kpiSystemBalance);
            assertNotNull(panel.kpiDifference);
            assertNotNull(panel.kpiPendingCount);

            assertEquals(6, panel.itemsTable.getColumnCount(), "A tabela de movimentos deve possuir 6 colunas");
            assertEquals(5, panel.operationsMenu.actionCount(), "O menu de operações deve possuir exactamente 5 acções");
        });
    }

    private void assertLineCount(Path file, int maxLines) throws IOException {
        assertTrue(Files.exists(file), file + " deve existir");
        long count = Files.lines(file).count();
        assertTrue(count <= maxLines,
                String.format("Ficheiro %s excedeu o teto de %d linhas (actual: %d)", file.getFileName(), maxLines, count));
    }
}

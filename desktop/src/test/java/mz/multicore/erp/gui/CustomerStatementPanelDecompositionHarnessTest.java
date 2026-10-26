package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: CustomerStatementPanel")
class CustomerStatementPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("CST-01: CustomerStatementPanel.java deve possuir <= 210 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("CustomerStatementPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 210,
                () -> "CustomerStatementPanel.java deve possuir <= 210 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CST-02: CustomerStatementOperationsCoordinator.java deve possuir <= 165 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path coord = ROOT.resolve("CustomerStatementOperationsCoordinator.java");
        List<String> lines = Files.readAllLines(coord);
        assertTrue(lines.size() <= 165,
                () -> "CustomerStatementOperationsCoordinator.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CST-03: Contratos literais canónicos preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("CustomerStatementPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertFalse(content.contains("new ModernPanel(12)"), "Não deve conter new ModernPanel(12)");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Conta Corrente & Reconciliação de Clientes\""),
                "Falta UIHelper.tableCardTop com título canónico");
        assertTrue(content.contains("TableQuickPeekController.install(table"), "Falta TableQuickPeekController.install(table");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "Falta UIHelper.ensureHeadersFit(table)");
        assertTrue(content.contains("ClientTablePagination.install(table)"), "Falta ClientTablePagination.install(table)");
        assertTrue(content.contains("KpiCard.createGrid(5)"), "Falta KpiCard.createGrid(5)");
    }

    @Test
    @DisplayName("CST-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("CustomerStatementPanel.java", "CustomerStatementOperationsCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("CST-05: Construtor público e classes aninhadas preservados")
    void testPublicApiPreserved() throws NoSuchMethodException, ClassNotFoundException {
        CustomerStatementPanel.class.getConstructor(
                mz.multicore.erp.desktop.client.AccountStatementApiClient.class,
                mz.multicore.erp.desktop.client.ComercialApiClient.class
        );

        Class<?> comboItem = Class.forName("mz.multicore.erp.gui.CustomerStatementPanel$ClientComboItem");
        assertTrue(comboItem.isRecord(), "ClientComboItem deve ser um record");
    }
}

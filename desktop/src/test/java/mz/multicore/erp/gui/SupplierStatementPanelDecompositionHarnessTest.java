package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: SupplierStatementPanel")
class SupplierStatementPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("SSP-01: SupplierStatementPanel.java deve possuir <= 165 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("SupplierStatementPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165,
                () -> "SupplierStatementPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("SSP-02: Coordenadores colaboradores devem possuir <= 110 linhas cada")
    void testCollaboratorLineCount() throws IOException {
        for (String file : List.of("SupplierStatementOperationsCoordinator.java", "SupplierStatementViewCoordinator.java")) {
            Path coord = ROOT.resolve(file);
            List<String> lines = Files.readAllLines(coord);
            assertTrue(lines.size() <= 110,
                    () -> file + " deve possuir <= 110 linhas. Atual: " + lines.size());
        }
    }

    @Test
    @DisplayName("SSP-03: Contratos literais canónicos preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("SupplierStatementPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertFalse(content.contains("new ModernPanel(12)"), "Não deve conter new ModernPanel(12)");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Conta Corrente & Reconciliação de Fornecedores\""),
                "Falta UIHelper.tableCardTop com título canónico");
        assertTrue(content.contains("TableQuickPeekController.install(table"), "Falta TableQuickPeekController.install(table");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "Falta UIHelper.ensureHeadersFit(table)");
        assertTrue(content.contains("ClientTablePagination.install(table)"), "Falta ClientTablePagination.install(table)");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Falta KpiCard.createGrid(4)");
    }

    @Test
    @DisplayName("SSP-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("SupplierStatementPanel.java", "SupplierStatementOperationsCoordinator.java", "SupplierStatementViewCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("SSP-05: Construtor público e classes aninhadas preservados")
    void testPublicApiPreserved() throws NoSuchMethodException, ClassNotFoundException {
        SupplierStatementPanel.class.getConstructor(
                mz.multicore.erp.desktop.client.AccountStatementApiClient.class,
                mz.multicore.erp.desktop.client.PurchaseApiClient.class
        );

        Class<?> comboItem = Class.forName("mz.multicore.erp.gui.SupplierStatementPanel$SupplierComboItem");
        assertTrue(comboItem.isRecord(), "SupplierComboItem deve ser um record");
    }
}

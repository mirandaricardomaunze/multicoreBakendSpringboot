package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: CRMPanel")
class CRMPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("CRM-DEC-01: CRMPanel.java deve possuir <= 205 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("CRMPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 205,
                () -> "CRMPanel.java deve possuir <= 205 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CRM-DEC-02: CrmViewCoordinator.java deve possuir <= 110 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path coord = ROOT.resolve("CrmViewCoordinator.java");
        List<String> lines = Files.readAllLines(coord);
        assertTrue(lines.size() <= 110,
                () -> "CrmViewCoordinator.java deve possuir <= 110 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CRM-DEC-03: Contratos canónicos literais preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("CRMPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Pedidos dos Clientes\""),
                "Falta UIHelper.tableCardTop com título Pedidos dos Clientes");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Folhas de Obra Registadas\", wsBar,\n                moreBtn, billBtn, newWsBtn)"),
                "Falta UIHelper.tableCardTop com formato exato para Folhas de Obra Registadas");
        assertTrue(content.contains("Acções da Folha"), "Falta ActionMenuButton Acções da Folha");
        assertTrue(content.contains("TableQuickPeekController.install(ticketsTable"), "Falta TableQuickPeekController.install(ticketsTable");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(ticketsTable)"), "Falta UIHelper.ensureHeadersFit(ticketsTable)");
    }

    @Test
    @DisplayName("CRM-DEC-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("CRMPanel.java", "CrmViewCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("CRM-DEC-05: Construtor público e métodos preservados")
    void testPublicApiPreserved() throws NoSuchMethodException {
        CRMPanel.class.getConstructor(
                mz.multicore.erp.desktop.client.CRMApiClient.class,
                mz.multicore.erp.desktop.client.ComercialApiClient.class
        );
        CRMPanel.class.getMethod("refreshData");
        CRMPanel.class.getMethod("onPanelSelected");
    }
}

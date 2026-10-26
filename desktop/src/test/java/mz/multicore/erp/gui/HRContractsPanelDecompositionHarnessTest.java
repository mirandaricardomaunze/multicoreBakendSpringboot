package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: HRContractsPanel")
class HRContractsPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("HRC-DEC-01: HRContractsPanel.java deve possuir <= 175 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("HRContractsPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 175,
                () -> "HRContractsPanel.java deve possuir <= 175 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("HRC-DEC-02: Colaboradores devem cumprir os seus tetos de linhas")
    void testCollaboratorLineCount() throws IOException {
        Path formDialog = ROOT.resolve("HrContractFormDialog.java");
        List<String> formLines = Files.readAllLines(formDialog);
        assertTrue(formLines.size() <= 140,
                () -> "HrContractFormDialog.java deve possuir <= 140 linhas. Atual: " + formLines.size());

        Path coord = ROOT.resolve("HRContractsOperationsCoordinator.java");
        List<String> coordLines = Files.readAllLines(coord);
        assertTrue(coordLines.size() <= 80,
                () -> "HRContractsOperationsCoordinator.java deve possuir <= 80 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("HRC-DEC-03: Contratos canónicos literais preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("HRContractsPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Contratos de Trabalho\""),
                "Falta UIHelper.tableCardTop com título canónico");
        assertTrue(content.contains("TableQuickPeekController.install(table"), "Falta TableQuickPeekController.install(table");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "Falta UIHelper.ensureHeadersFit(table)");
        assertTrue(content.contains("UIHelper.createActionMenuButton(\"Gestão do Contrato\")"),
                "Falta UIHelper.createActionMenuButton(\"Gestão do Contrato\")");
    }

    @Test
    @DisplayName("HRC-DEC-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("HRContractsPanel.java", "HrContractFormDialog.java", "HRContractsOperationsCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("HRC-DEC-05: Construtor package-private preservado")
    void testConstructorPreserved() throws NoSuchMethodException {
        HRContractsPanel.class.getDeclaredConstructor(HRPanel.class);
    }
}

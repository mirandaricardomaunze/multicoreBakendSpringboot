package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: HRTerminationsPanel")
class HRTerminationsPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("HRT-DEC-01: HRTerminationsPanel.java deve possuir <= 165 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("HRTerminationsPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165,
                () -> "HRTerminationsPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("HRT-DEC-02: Colaboradores devem cumprir os seus tetos de linhas")
    void testCollaboratorLineCount() throws IOException {
        Path formDialog = ROOT.resolve("HrTerminationFormDialog.java");
        List<String> formLines = Files.readAllLines(formDialog);
        assertTrue(formLines.size() <= 135,
                () -> "HrTerminationFormDialog.java deve possuir <= 135 linhas. Atual: " + formLines.size());

        Path coord = ROOT.resolve("HRTerminationsOperationsCoordinator.java");
        List<String> coordLines = Files.readAllLines(coord);
        assertTrue(coordLines.size() <= 90,
                () -> "HRTerminationsOperationsCoordinator.java deve possuir <= 90 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("HRT-DEC-03: Contratos canónicos literais preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("HRTerminationsPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Cessações e Acertos Finais\", bar,"),
                "Falta UIHelper.tableCardTop com título Cessações e Acertos Finais");
        assertTrue(content.contains("docsBtn, payBtn, newBtn)"), "Falta sequência de botões docsBtn, payBtn, newBtn");
        assertTrue(content.contains("Acerto PDF"), "Falta Acerto PDF no menu de documentos");
        assertTrue(content.contains("Certificado de Trabalho"), "Falta Certificado de Trabalho no menu");
        assertTrue(content.contains("TableQuickPeekController.install(table"), "Falta TableQuickPeekController.install(table");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"), "Falta UIHelper.ensureHeadersFit(table)");
    }

    @Test
    @DisplayName("HRT-DEC-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("HRTerminationsPanel.java", "HrTerminationFormDialog.java", "HRTerminationsOperationsCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("HRT-DEC-05: Construtor package-private preservado")
    void testConstructorPreserved() throws NoSuchMethodException {
        HRTerminationsPanel.class.getDeclaredConstructor(HRPanel.class);
    }
}

package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.HRApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HARNESS-HR-DEDUCTIONS-PANEL-DECOMPOSITION-001: Decomposição do Painel de Descontos e Empréstimos")
class HRDeductionsPanelDecompositionHarnessTest {

    @Test
    @DisplayName("DP-01: HRDeductionsPanel.java reduzido para <= 165 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/HRDeductionsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/HRDeductionsPanel.java");
        }
        assertTrue(Files.exists(path), "HRDeductionsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165, "HRDeductionsPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("DP-02: HrDeductionFormDialog.java desacoplado com <= 140 linhas")
    void testDialogLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/HrDeductionFormDialog.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/HrDeductionFormDialog.java");
        }
        assertTrue(Files.exists(path), "HrDeductionFormDialog.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 140, "HrDeductionFormDialog.java deve possuir <= 140 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("DP-03: Erradicação absoluta de JOptionPane em HRDeductionsPanel e HrDeductionFormDialog")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "HRDeductionsPanel.java",
                "HrDeductionFormDialog.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> content = Files.readAllLines(filePath);
            for (int i = 0; i < content.size(); i++) {
                assertFalse(content.get(i).contains("JOptionPane"),
                        file + " contém JOptionPane na linha " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("DP-04: Construtor package-private preservado e instanciável")
    void testConstructorAndBuildPanel() throws NoSuchMethodException {
        assertNotNull(HRDeductionsPanel.class.getDeclaredConstructor(HRPanel.class));
        HRPanel mockOwner = mock(HRPanel.class);
        HRDeductionsPanel panel = new HRDeductionsPanel(mockOwner);
        assertNotNull(panel, "HRDeductionsPanel deve ser instanciado");
    }

    @Test
    @DisplayName("DP-05: Invariantes canónicas de layout e ações preservadas")
    void testCanonicalInvariants() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/HRDeductionsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/HRDeductionsPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop(\"Descontos, Adiantamentos e Empréstimos\", bar,"),
                "Deve conter cabeçalho canónico");
        assertTrue(content.contains("refreshBtn, stopBtn, newDeductionBtn)"),
                "Deve manter ordem e agrupamento de botões");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"),
                "Deve calibrar colunas com ensureHeadersFit");
        assertTrue(content.contains("TableQuickPeekController.install(table"),
                "Deve instalar TableQuickPeekController");
        assertTrue(content.contains("Novo Desconto"),
                "Deve conter botão de novo desconto");
        assertTrue(content.contains("Adiantamento Salarial"),
                "Deve conter opção de adiantamento");
        assertTrue(content.contains("Empréstimo"),
                "Deve conter opção de empréstimo");
        assertTrue(content.contains("Desconto Recorrente"),
                "Deve conter opção de desconto recorrente");
    }
}

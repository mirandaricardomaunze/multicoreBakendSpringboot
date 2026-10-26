package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-HR-EXPENSES-PANEL-DECOMPOSITION-001: Decomposição do Painel de Despesas")
class HRExpensesPanelDecompositionHarnessTest {

    @Test
    @DisplayName("HEP-01: HRExpensesPanel.java reduzido para <= 75 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/HRExpensesPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/HRExpensesPanel.java");
        }
        assertTrue(Files.exists(path), "HRExpensesPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 75, "HRExpensesPanel.java deve possuir <= 75 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("HEP-02: Colaboradores desacoplados com limites rigorosos")
    void testCollaboratorsLineCount() throws IOException {
        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        Path dialogPath = baseDir.resolve("HrExpenseClaimDialog.java");
        assertTrue(Files.exists(dialogPath), "HrExpenseClaimDialog.java deve existir");
        List<String> dialogLines = Files.readAllLines(dialogPath);
        assertTrue(dialogLines.size() <= 75, "HrExpenseClaimDialog.java deve possuir <= 75 linhas. Atual: " + dialogLines.size());

        Path coordPath = baseDir.resolve("HrExpensesViewCoordinator.java");
        assertTrue(Files.exists(coordPath), "HrExpensesViewCoordinator.java deve existir");
        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 65, "HrExpensesViewCoordinator.java deve possuir <= 65 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("HEP-03: Erradicação absoluta de JOptionPane em HRExpensesPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "HRExpensesPanel.java",
                "HrExpenseClaimDialog.java",
                "HrExpensesViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                assertFalse(line.contains("JOptionPane"),
                        "Uso proibido de JOptionPane em " + file + " na linha " + (i + 1) + ": " + line);
            }
        }
    }

    @Test
    @DisplayName("HEP-04: Contrato de construtor e métodos operacionais preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                HRExpensesPanel.class.getDeclaredConstructor(HRPanel.class),
                "HRExpensesPanel deve possuir construtor com HRPanel");
        assertDoesNotThrow(() ->
                HRExpensesPanel.class.getDeclaredMethod("buildPanel"),
                "HRExpensesPanel deve possuir método buildPanel()");
        assertDoesNotThrow(() ->
                HRExpensesPanel.class.getDeclaredMethod("refresh"),
                "HRExpensesPanel deve possuir método refresh()");
    }
}

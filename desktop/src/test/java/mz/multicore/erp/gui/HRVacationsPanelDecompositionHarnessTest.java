package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-HR-VACATIONS-PANEL-DECOMPOSITION-001: Decomposição do Painel de Férias")
class HRVacationsPanelDecompositionHarnessTest {

    @Test
    @DisplayName("HVP-01: HRVacationsPanel.java reduzido para <= 110 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/HRVacationsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/HRVacationsPanel.java");
        }
        assertTrue(Files.exists(path), "HRVacationsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 110, "HRVacationsPanel.java deve possuir <= 110 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("HVP-02: Colaboradores desacoplados com limites rigorosos")
    void testCollaboratorsLineCount() throws IOException {
        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        Path dialogPath = baseDir.resolve("HrVacationFormDialog.java");
        assertTrue(Files.exists(dialogPath), "HrVacationFormDialog.java deve existir");
        List<String> dialogLines = Files.readAllLines(dialogPath);
        assertTrue(dialogLines.size() <= 90, "HrVacationFormDialog.java deve possuir <= 90 linhas. Atual: " + dialogLines.size());

        Path coordPath = baseDir.resolve("HrVacationsViewCoordinator.java");
        assertTrue(Files.exists(coordPath), "HrVacationsViewCoordinator.java deve existir");
        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 90, "HrVacationsViewCoordinator.java deve possuir <= 90 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("HVP-03: Erradicação absoluta de JOptionPane em HRVacationsPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "HRVacationsPanel.java",
                "HrVacationFormDialog.java",
                "HrVacationsViewCoordinator.java"
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
    @DisplayName("HVP-04: Contrato de construtor e métodos operacionais preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                HRVacationsPanel.class.getDeclaredConstructor(HRPanel.class),
                "HRVacationsPanel deve possuir construtor com HRPanel");
        assertDoesNotThrow(() ->
                HRVacationsPanel.class.getDeclaredMethod("buildPanel"),
                "HRVacationsPanel deve possuir método buildPanel()");
        assertDoesNotThrow(() ->
                HRVacationsPanel.class.getDeclaredMethod("load"),
                "HRVacationsPanel deve possuir método load()");
        assertDoesNotThrow(() ->
                HRVacationsPanel.class.getDeclaredMethod("openVacationAllowance"),
                "HRVacationsPanel deve possuir método openVacationAllowance()");
    }
}

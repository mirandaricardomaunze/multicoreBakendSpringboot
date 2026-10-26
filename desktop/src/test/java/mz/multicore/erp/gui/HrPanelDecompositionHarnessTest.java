package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de validação da decomposição modular do HRPanel
 * (HARNESS-HR-PANEL-DECOMPOSITION-001 / SPEC-HR-PANEL-DECOMPOSITION-001).
 */
public class HrPanelDecompositionHarnessTest {

    @Test
    @DisplayName("DECOMP-01: Limite de Linhas do HRPanel (<= 520 linhas)")
    void testHrPanelLineCount() throws IOException {
        Path path = findGuiFile("HRPanel.java");
        assertTrue(Files.exists(path), "HRPanel.java deve existir");
        long lines = Files.lines(path).count();
        assertTrue(lines <= 520,
                String.format("HRPanel.java deve ter <= 520 linhas; actual: %d", lines));
    }

    @Test
    @DisplayName("DECOMP-02: Limite de Linhas dos Componentes Especializados Decompostos")
    void testSpecializedComponentsLineCount() throws IOException {
        Path overviewPath = findGuiFile("HROverviewTabPanel.java");
        assertTrue(Files.exists(overviewPath), "HROverviewTabPanel.java deve existir");
        long ovLines = Files.lines(overviewPath).count();
        assertTrue(ovLines <= 200,
                String.format("HROverviewTabPanel.java deve ter <= 200 linhas; actual: %d", ovLines));

        Path employeeDialogPath = findGuiFile("HREmployeeFormDialog.java");
        assertTrue(Files.exists(employeeDialogPath), "HREmployeeFormDialog.java deve existir");
        long empLines = Files.lines(employeeDialogPath).count();
        assertTrue(empLines <= 200,
                String.format("HREmployeeFormDialog.java deve ter <= 200 linhas; actual: %d", empLines));

        Path payslipDialogPath = findGuiFile("HRPayslipFormDialog.java");
        assertTrue(Files.exists(payslipDialogPath), "HRPayslipFormDialog.java deve existir");
        long psLines = Files.lines(payslipDialogPath).count();
        assertTrue(psLines <= 120,
                String.format("HRPayslipFormDialog.java deve ter <= 120 linhas; actual: %d", psLines));

        Path absenceDialogPath = findGuiFile("HRAbsenceFormDialog.java");
        assertTrue(Files.exists(absenceDialogPath), "HRAbsenceFormDialog.java deve existir");
        long absLines = Files.lines(absenceDialogPath).count();
        assertTrue(absLines <= 100,
                String.format("HRAbsenceFormDialog.java deve ter <= 100 linhas; actual: %d", absLines));
    }

    private Path findGuiFile(String filename) {
        Path path = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", filename);
        if (!Files.exists(path)) {
            path = Path.of("desktop", "src", "main", "java", "mz", "multicore", "erp", "gui", filename);
        }
        return path;
    }
}

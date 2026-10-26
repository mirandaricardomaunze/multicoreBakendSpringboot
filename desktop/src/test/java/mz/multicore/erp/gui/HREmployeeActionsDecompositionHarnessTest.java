package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de HREmployeeActions (Fase 72).
 * Valida limites estritos de linhas, ausência de JOptionPane e contratos com drivers de ecrã.
 */
class HREmployeeActionsDecompositionHarnessTest {

    private static String readSource(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + relativePath, e);
        }
    }

    private static long countLines(String relativePath) {
        try {
            return Files.lines(Path.of(relativePath)).count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + relativePath, e);
        }
    }

    @Test
    @DisplayName("EAD-01: Limites físicos estritos de linhas de código")
    void ead01_limitesFisicosLinhasCodigo() {
        long actionsLines = countLines("src/main/java/mz/multicore/erp/gui/HREmployeeActions.java");
        long salaryDialogLines = countLines("src/main/java/mz/multicore/erp/gui/HREmployeeSalaryDialog.java");
        long documentsDialogLines = countLines("src/main/java/mz/multicore/erp/gui/HREmployeeDocumentsDialog.java");
        long healthDialogLines = countLines("src/main/java/mz/multicore/erp/gui/HREmployeeOccupationalHealthDialog.java");

        assertThat(actionsLines)
                .as("HREmployeeActions.java deve ter <= 100 linhas (reduzido de 663)")
                .isLessThanOrEqualTo(100);

        assertThat(salaryDialogLines)
                .as("HREmployeeSalaryDialog.java deve ter <= 180 linhas")
                .isLessThanOrEqualTo(180);

        assertThat(documentsDialogLines)
                .as("HREmployeeDocumentsDialog.java deve ter <= 160 linhas")
                .isLessThanOrEqualTo(160);

        assertThat(healthDialogLines)
                .as("HREmployeeOccupationalHealthDialog.java deve ter <= 320 linhas")
                .isLessThanOrEqualTo(320);
    }

    @Test
    @DisplayName("EAD-02: Zero diálogos legados JOptionPane")
    void ead02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/HREmployeeActions.java",
                "src/main/java/mz/multicore/erp/gui/HREmployeeSalaryDialog.java",
                "src/main/java/mz/multicore/erp/gui/HREmployeeDocumentsDialog.java",
                "src/main/java/mz/multicore/erp/gui/HREmployeeOccupationalHealthDialog.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("EAD-03: Delegação canónica para diálogos especializados")
    void ead03_delegacaoCanonica() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/HREmployeeActions.java");

        assertThat(actions)
                .contains("HREmployeeSalaryDialog.openSalaryHistory")
                .contains("HREmployeeSalaryDialog.openSalaryChangeDialog")
                .contains("HREmployeeDocumentsDialog.openDocuments")
                .contains("HREmployeeDocumentsDialog.openDocumentDialog")
                .contains("HREmployeeOccupationalHealthDialog.openOccupationalHealth")
                .contains("HREmployeeOccupationalHealthDialog.openOccupationalHealthAttachment")
                .contains("HREmployeeOccupationalHealthDialog.payOccupationalHealthExam")
                .contains("HREmployeeOccupationalHealthDialog.openOccupationalHealthCompliance")
                .contains("HREmployeeOccupationalHealthDialog.openOccupationalHealthForm");
    }

    @Test
    @DisplayName("EAD-04: Contratos e métodos públicos/package-private preservados")
    void ead04_contratosPreservados() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/HREmployeeActions.java");
        String salary = readSource("src/main/java/mz/multicore/erp/gui/HREmployeeSalaryDialog.java");
        String docs = readSource("src/main/java/mz/multicore/erp/gui/HREmployeeDocumentsDialog.java");
        String health = readSource("src/main/java/mz/multicore/erp/gui/HREmployeeOccupationalHealthDialog.java");

        assertThat(actions)
                .contains("void openSalaryHistory()")
                .contains("void openSalaryChangeDialog(")
                .contains("void openDocuments()")
                .contains("void openDocumentDialog(")
                .contains("void openOccupationalHealth()")
                .contains("void openOccupationalHealthAttachment(")
                .contains("void payOccupationalHealthExam(")
                .contains("void openOccupationalHealthCompliance()")
                .contains("void openOccupationalHealthForm(")
                .contains("void justifyAbsence()");

        assertThat(salary).contains("ModernFormDialog");
        assertThat(docs).contains("ModernFormDialog");
        assertThat(health)
                .contains("ModernFormDialog")
                .contains("ModernMessageDialog.confirm");
    }
}

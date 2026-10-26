package mz.multicore.erp.gui.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoalsTabDecompositionHarnessTest {

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
    @DisplayName("GTD-01: Limites físicos estritos de linhas de código")
    void gtd01_limitesFisicosLinhasCodigo() {
        long goalsTabLines = countLines("src/main/java/mz/multicore/erp/gui/performance/GoalsTab.java");
        long goalCreateDialogLines = countLines("src/main/java/mz/multicore/erp/gui/performance/GoalCreateDialog.java");

        assertThat(goalsTabLines)
                .as("GoalsTab.java deve ter <= 195 linhas (reduzido de 478 linhas)")
                .isLessThanOrEqualTo(195);

        assertThat(goalCreateDialogLines)
                .as("GoalCreateDialog.java deve ter <= 210 linhas")
                .isLessThanOrEqualTo(210);
    }

    @Test
    @DisplayName("GTD-02: Zero diálogos legados JOptionPane")
    void gtd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/performance/GoalsTab.java",
                "src/main/java/mz/multicore/erp/gui/performance/GoalCreateDialog.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("GTD-03: Delegação canónica para GoalCreateDialog.show")
    void gtd03_delegacaoParaGoalCreateDialog() {
        String src = readSource("src/main/java/mz/multicore/erp/gui/performance/GoalsTab.java");
        assertThat(src)
                .as("GoalsTab.java deve delegar a criação de metas para GoalCreateDialog.show")
                .contains("GoalCreateDialog.show(");
    }

    @Test
    @DisplayName("GTD-04: Contratos e layout corporativo preservados")
    void gtd04_contratosELayoutPreservados() {
        String goalsTab = readSource("src/main/java/mz/multicore/erp/gui/performance/GoalsTab.java");
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/performance/GoalCreateDialog.java");

        assertThat(goalsTab)
                .contains("record BusinessUnitOption")
                .contains("ModernPanel(16)")
                .contains("UIHelper.ensureHeadersFit(table)")
                .contains("TableQuickPeekController.install(table");

        assertThat(dialog)
                .contains("UIHelper.applyStandardModalBounds(dialog, 540, 600)")
                .contains("getActiveBusinessUnits")
                .contains("CreateSalesGoalRequest");
    }
}

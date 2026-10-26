package mz.multicore.erp.gui.pos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de PosShiftHandoverDialog (Fase 74).
 * Valida limites estritos de linhas, ausência de JOptionPane e contratos de passagem de turno.
 */
class PosShiftHandoverDialogDecompositionHarnessTest {

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
    @DisplayName("SHD-01: Limites físicos estritos de linhas de código")
    void shd01_limitesFisicosLinhasCodigo() {
        long dialogLines = countLines("src/main/java/mz/multicore/erp/gui/pos/PosShiftHandoverDialog.java");
        long resultLines = countLines("src/main/java/mz/multicore/erp/gui/pos/PosShiftHandoverResultPanel.java");

        assertThat(dialogLines)
                .as("PosShiftHandoverDialog.java deve ter <= 190 linhas (reduzido de 541)")
                .isLessThanOrEqualTo(190);

        assertThat(resultLines)
                .as("PosShiftHandoverResultPanel.java deve ter <= 120 linhas")
                .isLessThanOrEqualTo(120);
    }

    @Test
    @DisplayName("SHD-02: Zero diálogos legados JOptionPane")
    void shd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/pos/PosShiftHandoverDialog.java",
                "src/main/java/mz/multicore/erp/gui/pos/PosShiftHandoverResultPanel.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("SHD-03: Reutilização canónica de componentes de contagem e resultado")
    void shd03_reutilizacaoCanonicaComponentes() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/pos/PosShiftHandoverDialog.java");

        assertThat(dialog)
                .as("PosShiftHandoverDialog deve instanciar e reutilizar PosCashBreakdownPanel")
                .contains("new PosCashBreakdownPanel()")
                .contains("breakdownPanel.addTo(content)")
                .contains("breakdownPanel.calculateCountedAmount()")
                .contains("breakdownPanel.buildDenominationsJson()");

        assertThat(dialog)
                .as("PosShiftHandoverDialog deve instanciar PosShiftHandoverResultPanel")
                .contains("new PosShiftHandoverResultPanel()")
                .contains("content.add(resultPanel.component())")
                .contains("resultPanel.showResult(result)");
    }

    @Test
    @DisplayName("SHD-04: Preservação de contratos de passagem de turno")
    void shd04_contratosPreservados() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/pos/PosShiftHandoverDialog.java");

        assertThat(dialog)
                .contains("performHandover()")
                .contains("ShiftHandoverRequest")
                .contains("posApiClient.performShiftHandover")
                .contains("ToastManager.success")
                .doesNotContain("PdfFileSaver");
    }
}

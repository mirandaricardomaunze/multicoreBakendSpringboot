package mz.multicore.erp.gui.pos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de PosBlindCloseDialog (Fase 73).
 * Valida limites estritos de linhas, ausência de JOptionPane e contratos com harnesses POS.
 */
class PosBlindCloseDialogDecompositionHarnessTest {

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
    @DisplayName("BCD-01: Limites físicos estritos de linhas de código")
    void bcd01_limitesFisicosLinhasCodigo() {
        long dialogLines = countLines("src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseDialog.java");
        long breakdownLines = countLines("src/main/java/mz/multicore/erp/gui/pos/PosCashBreakdownPanel.java");
        long resultLines = countLines("src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseResultPanel.java");

        assertThat(dialogLines)
                .as("PosBlindCloseDialog.java deve ter <= 290 linhas (reduzido de 690)")
                .isLessThanOrEqualTo(290);

        assertThat(breakdownLines)
                .as("PosCashBreakdownPanel.java deve ter <= 270 linhas")
                .isLessThanOrEqualTo(270);

        assertThat(resultLines)
                .as("PosBlindCloseResultPanel.java deve ter <= 140 linhas")
                .isLessThanOrEqualTo(140);
    }

    @Test
    @DisplayName("BCD-02: Zero diálogos legados JOptionPane")
    void bcd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseDialog.java",
                "src/main/java/mz/multicore/erp/gui/pos/PosCashBreakdownPanel.java",
                "src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseResultPanel.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("BCD-03: Delegação canónica para componentes especializados")
    void bcd03_delegacaoCanonica() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseDialog.java");

        assertThat(dialog)
                .as("PosBlindCloseDialog deve instanciar PosCashBreakdownPanel")
                .contains("new PosCashBreakdownPanel()");

        assertThat(dialog)
                .as("PosBlindCloseDialog deve instanciar PosBlindCloseResultPanel")
                .contains("new PosBlindCloseResultPanel()");

        assertThat(dialog)
                .as("PosBlindCloseDialog deve integrar componentes ao container de conteúdo")
                .contains("breakdownPanel.addTo(root)")
                .contains("root.add(resultPanel.component())");
    }

    @Test
    @DisplayName("BCD-04: Preservação de contratos POS")
    void bcd04_contratosPreservados() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseDialog.java");

        assertThat(dialog)
                .contains("executeClose()")
                .contains("printZReport()")
                .contains("breakdownPanel.calculateCountedAmount()")
                .contains("breakdownPanel.buildDenominationsJson()")
                .contains("breakdownPanel.setControlsEnabled(false)")
                .contains("resultPanel.showResult(z, countedCash, notes)")
                .contains("SensitivePrintCoordinator.print")
                .doesNotContain("PdfFileSaver");
    }
}

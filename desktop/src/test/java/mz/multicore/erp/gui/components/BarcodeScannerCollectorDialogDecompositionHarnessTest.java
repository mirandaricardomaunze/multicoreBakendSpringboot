package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de BarcodeScannerCollectorDialog (Fase 80).
 * Valida limites estritos de linhas, ausência de JOptionPane, integridade do contrato público e delegações canónicas.
 */
class BarcodeScannerCollectorDialogDecompositionHarnessTest {

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
        long dialogLines = countLines("src/main/java/mz/multicore/erp/gui/components/BarcodeScannerCollectorDialog.java");
        long cardLines = countLines("src/main/java/mz/multicore/erp/gui/components/BarcodeScannerProductDetailsCard.java");
        long engineLines = countLines("src/main/java/mz/multicore/erp/gui/components/BarcodeScannerExecutionEngine.java");

        assertThat(dialogLines)
                .as("BarcodeScannerCollectorDialog.java deve ter <= 180 linhas (reduzido de 456)")
                .isLessThanOrEqualTo(180);

        assertThat(cardLines)
                .as("BarcodeScannerProductDetailsCard.java deve ter <= 150 linhas")
                .isLessThanOrEqualTo(150);

        assertThat(engineLines)
                .as("BarcodeScannerExecutionEngine.java deve ter <= 140 linhas")
                .isLessThanOrEqualTo(140);
    }

    @Test
    @DisplayName("BCD-02: Zero diálogos legados JOptionPane")
    void bcd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/components/BarcodeScannerCollectorDialog.java",
                "src/main/java/mz/multicore/erp/gui/components/BarcodeScannerProductDetailsCard.java",
                "src/main/java/mz/multicore/erp/gui/components/BarcodeScannerExecutionEngine.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("BCD-03: Preservação de enum CollectorMode, construtor canónico e método showDialog")
    void bcd03_contratosPreservados() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/components/BarcodeScannerCollectorDialog.java");

        assertThat(dialog)
                .contains("public enum CollectorMode")
                .contains("MANUFACTURING(\"Chão de Fábrica & Fabrico Industrial\", \"fas-industry\")")
                .contains("INVENTORY(\"Armazém & Auditoria de Stocks\", \"fas-boxes\")")
                .contains("public BarcodeScannerCollectorDialog(")
                .contains("public void showDialog()");
    }

    @Test
    @DisplayName("BCD-04: Delegação canónica para card de produto e motor de execução")
    void bcd04_delegacaoCanonica() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/components/BarcodeScannerCollectorDialog.java");

        assertThat(dialog)
                .contains("new BarcodeScannerProductDetailsCard(mode, e -> executeAction())")
                .contains("BarcodeScannerExecutionEngine.searchProduct(")
                .contains("BarcodeScannerExecutionEngine.checkBomAndEnable(")
                .contains("BarcodeScannerExecutionEngine.launchProductionOrder(")
                .contains("UIHelper.applyStandardModalBounds(dialog, 840, 600)");
    }
}

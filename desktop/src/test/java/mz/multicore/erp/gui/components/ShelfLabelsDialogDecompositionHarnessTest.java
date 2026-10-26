package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de ShelfLabelsDialog (Fase 79).
 * Valida limites estritos de linhas, ausência de JOptionPane, integridade do contrato público e delegações canónicas.
 */
class ShelfLabelsDialogDecompositionHarnessTest {

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
    @DisplayName("SLD-01: Limites físicos estritos de linhas de código")
    void sld01_limitesFisicosLinhasCodigo() {
        long dialogLines = countLines("src/main/java/mz/multicore/erp/gui/components/ShelfLabelsDialog.java");
        long rendererLines = countLines("src/main/java/mz/multicore/erp/gui/components/ShelfLabelCardRenderer.java");
        long engineLines = countLines("src/main/java/mz/multicore/erp/gui/components/ShelfLabelPrintEngine.java");

        assertThat(dialogLines)
                .as("ShelfLabelsDialog.java deve ter <= 209 linhas (reduzido de 500)")
                .isLessThanOrEqualTo(209);

        assertThat(rendererLines)
                .as("ShelfLabelCardRenderer.java deve ter <= 160 linhas")
                .isLessThanOrEqualTo(160);

        assertThat(engineLines)
                .as("ShelfLabelPrintEngine.java deve ter <= 85 linhas")
                .isLessThanOrEqualTo(85);
    }

    @Test
    @DisplayName("SLD-02: Zero diálogos legados JOptionPane")
    void sld02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/components/ShelfLabelsDialog.java",
                "src/main/java/mz/multicore/erp/gui/components/ShelfLabelCardRenderer.java",
                "src/main/java/mz/multicore/erp/gui/components/ShelfLabelPrintEngine.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("SLD-03: Preservação de enum, construtores e métodos públicos estáticos")
    void sld03_contratosPreservados() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/components/ShelfLabelsDialog.java");

        assertThat(dialog)
                .contains("public enum PackagingLevel")
                .contains("public ShelfLabelsDialog(Window owner, List<ProductDTO> products)")
                .contains("public ShelfLabelsDialog(Window owner, List<ProductDTO> products, String defaultBatch, int defaultCopies)")
                .contains("public static void show(Window owner, List<ProductDTO> products)")
                .contains("public static void showWithBatch(Window owner, List<ProductDTO> products, String batchNumber, int defaultCopies)")
                .contains("public static JPanel createSingleLabelCard(ProductDTO product)")
                .contains("public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level)")
                .contains("public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level, String batchNumber, String expiryDate)");
    }

    @Test
    @DisplayName("SLD-04: Delegação canónica para renderizador de cartões e motor de impressão")
    void sld04_delegacaoCanonica() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/components/ShelfLabelsDialog.java");

        assertThat(dialog)
                .contains("ShelfLabelCardRenderer.createSingleLabelCard(")
                .contains("ShelfLabelPrintEngine.printLabels(");
    }
}

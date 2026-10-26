package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de StockProductFormDialog (Fase 82).
 * Valida limites estritos de linhas, ausência de JOptionPane, integridade do contrato estático e delegações canónicas.
 */
class StockProductFormDialogDecompositionHarnessTest {

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
    @DisplayName("SPFD-01: Limites físicos estritos de linhas de código")
    void spfd01_limitesFisicosLinhasCodigo() {
        long dialogLines = countLines("src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java");
        long builderLines = countLines("src/main/java/mz/multicore/erp/gui/StockProductFormBuilder.java");
        long validatorLines = countLines("src/main/java/mz/multicore/erp/gui/StockProductFormValidator.java");

        assertThat(dialogLines)
                .as("StockProductFormDialog.java deve ter <= 195 linhas (reduzido de 440)")
                .isLessThanOrEqualTo(195);

        assertThat(builderLines)
                .as("StockProductFormBuilder.java deve ter <= 150 linhas")
                .isLessThanOrEqualTo(150);

        assertThat(validatorLines)
                .as("StockProductFormValidator.java deve ter <= 140 linhas")
                .isLessThanOrEqualTo(140);
    }

    @Test
    @DisplayName("SPFD-02: Zero diálogos legados JOptionPane")
    void spfd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java",
                "src/main/java/mz/multicore/erp/gui/StockProductFormBuilder.java",
                "src/main/java/mz/multicore/erp/gui/StockProductFormValidator.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("SPFD-03: Preservação de métodos públicos estáticos e contratos auditados")
    void spfd03_contratosPreservados() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java");

        assertThat(dialog)
                .contains("static void showCreate(")
                .contains("static void showEdit(")
                .contains(".setConfirmButton(\"Actualizar\", \"fas-save\")")
                .contains("dialog.setOnSaveAsync");
    }

    @Test
    @DisplayName("SPFD-04: Delegação canónica para montador de formulários e validador")
    void spfd04_delegacaoCanonica() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java");

        assertThat(dialog)
                .contains("StockProductFormBuilder.bindPackagingFields(")
                .contains("StockProductFormBuilder.createImageChooserPanel(")
                .contains("StockProductFormValidator.validateForm(");
    }
}

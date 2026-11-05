package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de StockProductActions (Fase 70).
 * Valida limites estritos de linhas, ausência de JOptionPane e contratos com o StockInteractionHarnessTest.
 */
class StockProductActionsDecompositionHarnessTest {

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
    @DisplayName("SPAD-01: Limites físicos estritos de linhas de código")
    void spad01_limitesFisicosLinhasCodigo() {
        long actionsLines = countLines("src/main/java/mz/multicore/erp/gui/StockProductActions.java");
        long batchDialogLines = countLines("src/main/java/mz/multicore/erp/gui/StockBatchEntryDialog.java");
        long productFormLines = countLines("src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java");

        assertThat(actionsLines)
                .as("StockProductActions.java deve ter <= 120 linhas (reduzido de 735 linhas)")
                .isLessThanOrEqualTo(120);

        assertThat(batchDialogLines)
                .as("StockBatchEntryDialog.java deve ter <= 190 linhas")
                .isLessThanOrEqualTo(190);

        assertThat(productFormLines)
                .as("StockProductFormDialog.java deve ter <= 450 linhas")
                .isLessThanOrEqualTo(450);
    }

    @Test
    @DisplayName("SPAD-02: Zero diálogos legados JOptionPane")
    void spad02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/StockProductActions.java",
                "src/main/java/mz/multicore/erp/gui/StockBatchEntryDialog.java",
                "src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("SPAD-03: Delegação canónica para diálogos especializados")
    void spad03_delegacaoCanonicaParaDialogos() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/StockProductActions.java");

        assertThat(actions)
                .as("StockProductActions deve delegar para StockBatchEntryDialog.show(...)")
                .contains("StockBatchEntryDialog.show(");

        assertThat(actions)
                .as("StockProductActions deve delegar criação para StockProductFormDialog.showCreate(...)")
                .contains("StockProductFormDialog.showCreate(");

        assertThat(actions)
                .as("StockProductActions deve delegar edição para StockProductFormDialog.showEdit(...)")
                .contains("StockProductFormDialog.showEdit(");
    }

    @Test
    @DisplayName("SPAD-04: Contratos e expectativas de auditoria preservados")
    void spad04_contratosPreservados() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/StockProductActions.java");
        String formDialog = readSource("src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java");
        String batchDialog = readSource("src/main/java/mz/multicore/erp/gui/StockBatchEntryDialog.java");

        assertThat(actions)
                .contains(".setConfirmButton(\"Actualizar\", \"fas-save\")")
                .contains("dialog.setOnSaveAsync");

        assertThat(formDialog)
                .contains(".setConfirmButton(\"Actualizar\", \"fas-save\")")
                .contains("dialog.setOnSaveAsync");

        // Lote vencido é recusado logo no diálogo (como no servidor); validade só obrigatória em perecíveis.
        assertThat(batchDialog)
                .contains("\"Validade expirada\"")
                .contains("selectedDTO.tracksExpiry()")
                .contains("ModernFormDialog");
    }
}

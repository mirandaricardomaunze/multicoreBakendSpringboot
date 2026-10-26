package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockTransferActionsDecompositionHarnessTest {

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
    @DisplayName("STAD-01: Limites físicos estritos de linhas de código")
    void stad01_limitesFisicosLinhasCodigo() {
        long actionsLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferActions.java");
        long linesDialogLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferLinesDialog.java");
        long createDialogLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferCreateModalDialog.java");

        assertThat(actionsLines)
                .as("StockTransferActions.java deve ter <= 360 linhas (reduzido de 935 linhas)")
                .isLessThanOrEqualTo(360);

        assertThat(linesDialogLines)
                .as("StockTransferLinesDialog.java deve ter <= 190 linhas")
                .isLessThanOrEqualTo(190);

        assertThat(createDialogLines)
                .as("StockTransferCreateModalDialog.java deve ter <= 450 linhas")
                .isLessThanOrEqualTo(450);
    }

    @Test
    @DisplayName("STAD-02: Zero diálogos legados JOptionPane")
    void stad02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/StockTransferActions.java",
                "src/main/java/mz/multicore/erp/gui/StockTransferLinesDialog.java",
                "src/main/java/mz/multicore/erp/gui/StockTransferCreateModalDialog.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("STAD-03: Delegação canónica para diálogos especializados")
    void stad03_delegacaoCanonicaParaDialogos() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/StockTransferActions.java");

        assertThat(actions)
                .as("StockTransferActions deve delegar para StockTransferCreateModalDialog.show(owner)")
                .contains("StockTransferCreateModalDialog.show(owner);");

        assertThat(actions)
                .as("StockTransferActions deve delegar para StockTransferLinesDialog.show(...)")
                .contains("StockTransferLinesDialog.show(");
    }

    @Test
    @DisplayName("STAD-04: Contratos e integrações corporativas preservados")
    void stad04_contratosPreservados() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/StockTransferActions.java");
        String linesDlg = readSource("src/main/java/mz/multicore/erp/gui/StockTransferLinesDialog.java");
        String createDlg = readSource("src/main/java/mz/multicore/erp/gui/StockTransferCreateModalDialog.java");

        assertThat(actions)
                .contains("StockTransferReceivingDialog.show(")
                .contains("ModernMessageDialog.confirm(")
                .doesNotContain("linesWrap.add(lineButtons, BorderLayout.SOUTH)");

        assertThat(linesDlg)
                .contains("new ModernPanel(16)")
                .contains("StockTransferVolumeLabelsDialog.show(")
                .contains("createTotalBadge(\"Total Geral\"");

        assertThat(createDlg)
                .contains("ModernPanel")
                .contains("findNextFEFO")
                .contains("ModernFormDialog");
    }
}

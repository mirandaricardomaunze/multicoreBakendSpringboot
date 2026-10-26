package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de ApprovalDetailsDialog (Fase 76).
 * Valida limites estritos de linhas, ausência de JOptionPane e delegações canónicas.
 */
class ApprovalDetailsDialogDecompositionHarnessTest {

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
    @DisplayName("ADD-01: Limites físicos estritos de linhas de código")
    void add01_limitesFisicosLinhasCodigo() {
        long dialogLines = countLines("src/main/java/mz/multicore/erp/gui/ApprovalDetailsDialog.java");
        long viewLines = countLines("src/main/java/mz/multicore/erp/gui/ApprovalDocumentDetailsView.java");

        assertThat(dialogLines)
                .as("ApprovalDetailsDialog.java deve ter <= 190 linhas (reduzido de 486)")
                .isLessThanOrEqualTo(190);

        assertThat(viewLines)
                .as("ApprovalDocumentDetailsView.java deve ter <= 185 linhas")
                .isLessThanOrEqualTo(185);
    }

    @Test
    @DisplayName("ADD-02: Zero diálogos legados JOptionPane")
    void add02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/ApprovalDetailsDialog.java",
                "src/main/java/mz/multicore/erp/gui/ApprovalDocumentDetailsView.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("ADD-03: Delegação canónica para ApprovalDocumentDetailsView")
    void add03_delegacaoCanonica() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/ApprovalDetailsDialog.java");

        assertThat(dialog)
                .as("ApprovalDetailsDialog deve delegar a renderização de detalhes do documento para ApprovalDocumentDetailsView")
                .contains("ApprovalDocumentDetailsView.buildTab(");
    }

    @Test
    @DisplayName("ADD-04: Preservação de contratos executivos e métodos públicos")
    void add04_contratosPreservados() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/ApprovalDetailsDialog.java");

        assertThat(dialog)
                .contains("public static void show(")
                .contains("handleReject(")
                .contains("handleApprove(")
                .contains("ModernMessageDialog.confirm")
                .contains("UIHelper.promptRequiredText")
                .contains("ToastManager.success");
    }
}

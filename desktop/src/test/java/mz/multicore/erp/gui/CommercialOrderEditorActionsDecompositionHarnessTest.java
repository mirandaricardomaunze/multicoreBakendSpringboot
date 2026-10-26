package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de CommercialOrderEditorActions (Fase 81).
 * Valida limites estritos de linhas, ausência de JOptionPane, integridade do contrato estático e delegações canónicas.
 */
class CommercialOrderEditorActionsDecompositionHarnessTest {

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
    @DisplayName("COEA-01: Limites físicos estritos de linhas de código")
    void coea01_limitesFisicosLinhasCodigo() {
        long actionsLines = countLines("src/main/java/mz/multicore/erp/gui/CommercialOrderEditorActions.java");
        long creditLines = countLines("src/main/java/mz/multicore/erp/gui/CommercialOrderCreditPolicy.java");
        long lifeLines = countLines("src/main/java/mz/multicore/erp/gui/CommercialOrderLifecycleCoordinator.java");
        long barcodeLines = countLines("src/main/java/mz/multicore/erp/gui/CommercialOrderBarcodeAndLookupHandler.java");

        assertThat(actionsLines)
                .as("CommercialOrderEditorActions.java deve ter <= 210 linhas (reduzido de 459)")
                .isLessThanOrEqualTo(210);

        assertThat(creditLines)
                .as("CommercialOrderCreditPolicy.java deve ter <= 120 linhas")
                .isLessThanOrEqualTo(120);

        assertThat(lifeLines)
                .as("CommercialOrderLifecycleCoordinator.java deve ter <= 75 linhas")
                .isLessThanOrEqualTo(75);

        assertThat(barcodeLines)
                .as("CommercialOrderBarcodeAndLookupHandler.java deve ter <= 110 linhas")
                .isLessThanOrEqualTo(110);
    }

    @Test
    @DisplayName("COEA-02: Zero diálogos legados JOptionPane")
    void coea02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/CommercialOrderEditorActions.java",
                "src/main/java/mz/multicore/erp/gui/CommercialOrderCreditPolicy.java",
                "src/main/java/mz/multicore/erp/gui/CommercialOrderLifecycleCoordinator.java",
                "src/main/java/mz/multicore/erp/gui/CommercialOrderBarcodeAndLookupHandler.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("COEA-03: Preservação de métodos públicos estáticos de compatibilidade")
    void coea03_contratosPreservados() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/CommercialOrderEditorActions.java");

        assertThat(actions)
                .contains("static void addBlankLine(")
                .contains("static void handleBarcodeScanned(")
                .contains("static void syncLineFromGrid(")
                .contains("static void removeSelectedLine(")
                .contains("static void applyProductsFromLookup(")
                .contains("static void handleClientChanged(")
                .contains("static void ensurePaymentTermsOption(")
                .contains("static void openSelected(")
                .contains("static void load(")
                .contains("static void refreshTable(")
                .contains("static void stopCellEditing(")
                .contains("static BigDecimal previewPrice(");
    }

    @Test
    @DisplayName("COEA-04: Delegação canónica para os colaboradores desacoplados")
    void coea04_delegacaoCanonica() {
        String actions = readSource("src/main/java/mz/multicore/erp/gui/CommercialOrderEditorActions.java");

        assertThat(actions)
                .contains("CommercialOrderLifecycleCoordinator.openSelected(")
                .contains("CommercialOrderCreditPolicy.handleClientChanged(")
                .contains("CommercialOrderCreditPolicy.ensurePaymentTermsOption(")
                .contains("CommercialOrderBarcodeAndLookupHandler.handleBarcodeScanned(")
                .contains("CommercialOrderBarcodeAndLookupHandler.applyProductsFromLookup(");
    }
}

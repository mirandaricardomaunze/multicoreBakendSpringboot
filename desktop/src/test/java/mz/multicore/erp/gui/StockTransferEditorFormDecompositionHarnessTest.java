package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de StockTransferEditorForm (Fase 90).
 * Valida limites estritos de linhas, ausência de JOptionPane, e delegação para helpers especializados.
 */
class StockTransferEditorFormDecompositionHarnessTest {

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
    @DisplayName("STED-01: Limites físicos estritos de linhas de código")
    void sted01_limitesFisicosLinhasCodigo() {
        long editorLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferEditorForm.java");
        long headerLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferEditorHeaderSection.java");
        long gridLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferEditorGridConfigurator.java");
        long catalogLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferCatalogCoordinator.java");
        long replenishmentLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferReplenishmentHelper.java");
        long calculatorLines = countLines("src/main/java/mz/multicore/erp/gui/StockTransferEditorRowCalculator.java");

        assertThat(editorLines)
                .as("StockTransferEditorForm.java deve ter <= 420 linhas (reduzido de 546 linhas)")
                .isLessThanOrEqualTo(420);

        assertThat(headerLines)
                .as("StockTransferEditorHeaderSection.java deve ter <= 85 linhas")
                .isLessThanOrEqualTo(85);

        assertThat(gridLines)
                .as("StockTransferEditorGridConfigurator.java deve ter <= 65 linhas")
                .isLessThanOrEqualTo(65);

        assertThat(catalogLines)
                .as("StockTransferCatalogCoordinator.java deve ter <= 110 linhas")
                .isLessThanOrEqualTo(110);

        assertThat(replenishmentLines)
                .as("StockTransferReplenishmentHelper.java deve ter <= 130 linhas")
                .isLessThanOrEqualTo(130);

        assertThat(calculatorLines)
                .as("StockTransferEditorRowCalculator.java deve ter <= 120 linhas")
                .isLessThanOrEqualTo(120);
    }

    @Test
    @DisplayName("STED-02: Zero diálogos legados JOptionPane")
    void sted02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/StockTransferEditorForm.java",
                "src/main/java/mz/multicore/erp/gui/StockTransferEditorHeaderSection.java",
                "src/main/java/mz/multicore/erp/gui/StockTransferEditorGridConfigurator.java",
                "src/main/java/mz/multicore/erp/gui/StockTransferCatalogCoordinator.java",
                "src/main/java/mz/multicore/erp/gui/StockTransferReplenishmentHelper.java",
                "src/main/java/mz/multicore/erp/gui/StockTransferEditorRowCalculator.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("STED-03: Delegação canónica para helpers especializados")
    void sted03_delegacaoCanonicaParaHelpers() {
        String editor = readSource("src/main/java/mz/multicore/erp/gui/StockTransferEditorForm.java");

        assertThat(editor)
                .as("StockTransferEditorForm deve delegar layout de cabeçalho para StockTransferEditorHeaderSection")
                .contains("StockTransferEditorHeaderSection.build(");

        assertThat(editor)
                .as("StockTransferEditorForm deve delegar configuração de grelha para StockTransferEditorGridConfigurator")
                .contains("StockTransferEditorGridConfigurator.configure(");

        assertThat(editor)
                .as("StockTransferEditorForm deve delegar abertura de catálogo para StockTransferCatalogCoordinator")
                .contains("StockTransferCatalogCoordinator.openCatalog(");

        assertThat(editor)
                .as("StockTransferEditorForm deve delegar leitor de código de barras para StockTransferCatalogCoordinator")
                .contains("StockTransferCatalogCoordinator.handleBarcodeScanned(");

        assertThat(editor)
                .as("StockTransferEditorForm deve delegar sugestão de reposição para StockTransferReplenishmentHelper")
                .contains("StockTransferReplenishmentHelper.suggest(");

        assertThat(editor)
                .as("StockTransferEditorForm deve delegar recálculo de linhas para StockTransferEditorRowCalculator")
                .contains("StockTransferEditorRowCalculator.recalculate(");

        assertThat(editor)
                .as("StockTransferEditorForm deve delegar totais para StockTransferEditorRowCalculator")
                .contains("StockTransferEditorRowCalculator.updateTotals(");
    }

    @Test
    @DisplayName("STED-04: Contratos e modelo de dados preservados")
    void sted04_contratosPreservados() {
        String editor = readSource("src/main/java/mz/multicore/erp/gui/StockTransferEditorForm.java");
        String replenishment = readSource("src/main/java/mz/multicore/erp/gui/StockTransferReplenishmentHelper.java");
        String calculator = readSource("src/main/java/mz/multicore/erp/gui/StockTransferEditorRowCalculator.java");

        assertThat(editor)
                .contains("requiresReceiptCheck = new JCheckBox(\"Exigir confirmação de recepção no destino (stock em trânsito)\")")
                .contains("COL_PRODUCT")
                .contains("COL_QUANTITY")
                .contains("COL_TOTAL");

        assertThat(replenishment)
                .contains("static void suggest(")
                .contains("record ReplenishmentItem(ProductDTO product, BigDecimal qtyNeeded");

        assertThat(calculator)
                .contains("static void recalculate(")
                .contains("static void updateTotals(")
                .contains("LogisticsLoadCalculator.recommendedVehicleType(");
    }
}

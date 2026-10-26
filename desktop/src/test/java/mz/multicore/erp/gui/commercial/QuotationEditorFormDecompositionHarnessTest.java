package mz.multicore.erp.gui.commercial;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de QuotationEditorForm (Fase 71).
 * Valida limites estritos de linhas, ausência de JOptionPane e contratos com harnesses PHC.
 */
class QuotationEditorFormDecompositionHarnessTest {

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
    @DisplayName("QED-01: Limites físicos estritos de linhas de código")
    void qed01_limitesFisicosLinhasCodigo() {
        long formLines = countLines("src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorForm.java");
        long headerFormLines = countLines("src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorHeaderForm.java");
        long rowCalculatorLines = countLines("src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorRowCalculator.java");

        assertThat(formLines)
                .as("QuotationEditorForm.java deve ter <= 380 linhas (reduzido de 666)")
                .isLessThanOrEqualTo(380);

        assertThat(headerFormLines)
                .as("QuotationEditorHeaderForm.java deve ter <= 210 linhas")
                .isLessThanOrEqualTo(210);

        assertThat(rowCalculatorLines)
                .as("QuotationEditorRowCalculator.java deve ter <= 110 linhas")
                .isLessThanOrEqualTo(110);
    }

    @Test
    @DisplayName("QED-02: Zero diálogos legados JOptionPane")
    void qed02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorForm.java",
                "src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorHeaderForm.java",
                "src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorRowCalculator.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("QED-03: Delegação canónica para formulário de cabeçalho e calculadora de linhas")
    void qed03_delegacaoCanonica() {
        String form = readSource("src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorForm.java");

        assertThat(form)
                .as("QuotationEditorForm deve conter instância de QuotationEditorHeaderForm")
                .contains("new QuotationEditorHeaderForm()");

        assertThat(form)
                .as("QuotationEditorForm deve delegar validação do cabeçalho")
                .contains("headerForm.validateHeader()");

        assertThat(form)
                .as("QuotationEditorForm deve usar QuotationEditorRowCalculator")
                .contains("QuotationEditorRowCalculator.calculate")
                .contains("QuotationEditorRowCalculator.resolveProduct");
    }

    @Test
    @DisplayName("QED-04: Preservação de contratos PHC de grelha inline")
    void qed04_contratosPreservados() {
        String form = readSource("src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorForm.java");

        assertThat(form)
                .contains("stopCellEditing()")
                .contains("for (int row = 0; row < draftLines.size(); row++)")
                .contains("packagesPerBox()", "unitsPerPackage()", "multiply(unitsPerBox)")
                .contains("installDocumentGridShortcuts(linesTable")
                .contains("return tableEditable && (column <= 3 || column == 6)")
                .contains("syncLineFromGrid")
                .contains("Adicionar linha")
                .contains("Remover item")
                .contains("UpdateQuotationRequest")
                .doesNotContain("JPanel lineForm");
    }
}

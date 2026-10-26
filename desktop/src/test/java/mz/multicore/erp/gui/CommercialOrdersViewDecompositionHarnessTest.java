package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommercialOrdersViewDecompositionHarnessTest {

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
    @DisplayName("COVD-01: Limites físicos estritos de linhas de código")
    void covd01_limitesFisicosLinhasCodigo() {
        long ordersViewLines = countLines("src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java");
        long headerFormLines = countLines("src/main/java/mz/multicore/erp/gui/CommercialOrderHeaderForm.java");

        assertThat(ordersViewLines)
                .as("CommercialOrdersView.java deve ter <= 360 linhas (reduzido de 516 linhas)")
                .isLessThanOrEqualTo(360);

        assertThat(headerFormLines)
                .as("CommercialOrderHeaderForm.java deve ter <= 210 linhas")
                .isLessThanOrEqualTo(210);
    }

    @Test
    @DisplayName("COVD-02: Zero diálogos legados JOptionPane")
    void covd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java",
                "src/main/java/mz/multicore/erp/gui/CommercialOrderHeaderForm.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("COVD-03: Delegação canónica para CommercialOrderHeaderForm")
    void covd03_delegacaoParaCommercialOrderHeaderForm() {
        String src = readSource("src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java");
        assertThat(src)
                .as("CommercialOrdersView.java deve delegar a criação do cabeçalho a CommercialOrderHeaderForm")
                .contains("CommercialOrderHeaderForm.createHeaderCard(owner)");
    }

    @Test
    @DisplayName("COVD-04: Contratos corporativos e form controls preservados")
    void covd04_contratosELayoutPreservados() {
        String ordersView = readSource("src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java");
        String headerForm = readSource("src/main/java/mz/multicore/erp/gui/CommercialOrderHeaderForm.java");

        assertThat(ordersView)
                .contains("owner.orderEditor = new DocumentEditorHost(")
                .contains("owner.encomendasHost.add(owner.orderEditor, \"editor\")")
                .contains("UIHelper.tableCardTop(\"Itens da Encomenda\"")
                .contains("UIHelper.ensureHeadersFit(owner.ordersTable)");

        assertThat(headerForm)
                .contains("new ModernPanel(16)")
                .contains("owner.orderClientCombo")
                .contains("owner.orderClientWalkInField")
                .contains("owner.orderPaymentTermsCombo")
                .contains("owner.orderWarehouseCombo")
                .contains("owner.orderKindCombo")
                .contains("owner.orderDestinationCombo")
                .contains("owner.orderNotesField")
                .contains("owner.markOrderEditorDirty()");
    }
}

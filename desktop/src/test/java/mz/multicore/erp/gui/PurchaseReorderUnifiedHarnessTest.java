package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness REORDER-01..05: a Reposição Inteligente segue o padrão canónico de tabelas e KPIs. */
class PurchaseReorderUnifiedHarnessTest {

    private static String source() throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/PurchaseReorderPanel.java"));
    }

    @Test
    @DisplayName("REORDER-01: KPIs usam KpiCard canónico e não cartões privados")
    void reorder01_canonicalKpis() throws Exception {
        String s = source();
        assertThat(s).contains("KpiCard.createGrid(4)").contains("KpiCard.createInteractiveCard(");
        assertThat(s).doesNotContain("createKpiCard(").doesNotContain("new ModernPanel(10)");
    }

    @Test
    @DisplayName("REORDER-02: título e acções vivem apenas no card (sem cabeçalho duplicado)")
    void reorder02_singleHeader() throws Exception {
        String s = source();
        assertThat(s).contains("UIHelper.tableCardTop(").contains("new ModernPanel(16)");
        assertThat(s).doesNotContain("UIHelper.createHeading(").doesNotContain("GridBagLayout");
    }

    @Test
    @DisplayName("REORDER-03: Quick Peek instalado na tabela")
    void reorder03_quickPeek() throws Exception {
        assertThat(source()).contains("TableQuickPeekController.install(reorderTable");
    }

    @Test
    @DisplayName("REORDER-04: KPIs filtram por urgência")
    void reorder04_drilldown() throws Exception {
        assertThat(source()).contains("filterUrgency(\"ESGOTADO\")")
                .contains("filterUrgency(\"CRÍTICO\")").contains("filterUrgency(null)");
    }

    @Test
    @DisplayName("REORDER-05: larguras de colunas calibradas e cabeçalhos ajustados")
    void reorder05_columnsFit() throws Exception {
        assertThat(source()).contains("setPreferredWidth(widths[i])").contains("UIHelper.ensureHeadersFit(reorderTable)");
    }
}

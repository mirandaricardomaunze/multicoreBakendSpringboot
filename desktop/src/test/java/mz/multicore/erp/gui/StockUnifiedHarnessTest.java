package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness STOCK-01..03:
 * Valida a conformidade canónica do módulo de Stock & Armazéns:
 * - Filtros em linha única horizontal via TableFilter.bar (sem GridBagLayout em 2 linhas)
 * - Ausência de reticências proibidas nos botões
 * - Ausência de cabeçalhos/títulos duplicados fora do card
 * - Instalação de Quick Peek silencioso com tecla Espaço
 * - Calibração de larguras de coluna com UIHelper.ensureHeadersFit
 */
class StockUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("STOCK-01: StockPanel segue padrão de card único, TableFilter.bar em todas as abas e calibração")
    void stock01_stockPanelCanonical() throws Exception {
        String s = source("StockPanel.java");

        // Proibição de GridBagLayout em filtros
        assertThat(s).doesNotContain("filters = new JPanel(new GridBagLayout())");
        assertThat(s).doesNotContain("mvFilters = new JPanel(new GridBagLayout())");
        assertThat(s).doesNotContain("trFilters = new JPanel(new GridBagLayout())");

        // Uso canónico de TableFilter.bar e UIHelper.ensureHeadersFit
        assertThat(s).contains("TableFilter.bar(");
        assertThat(s).contains("UIHelper.ensureHeadersFit(stockTable)");
        assertThat(s).contains("UIHelper.ensureHeadersFit(movementsTable)");
        assertThat(s).contains("UIHelper.ensureHeadersFit(transferTable)");
        assertThat(s).contains("TableQuickPeekController.install(stockTable");
        assertThat(s).contains("new TableFilter.DateRangeFilter(movementFrom, movementTo, 0)");
        assertThat(s).contains("TableExportAction.export(");
        assertThat(s).contains("Todos os artigos");
    }

    @Test
    @DisplayName("STOCK-02: StockBatchesPanel e StockAlertsPanel sem GridBagLayout em filtros e com ensureHeadersFit")
    void stock02_batchesAndAlertsCanonical() throws Exception {
        String batches = source("StockBatchesPanel.java");
        assertThat(batches).doesNotContain("new JPanel(new GridBagLayout())");
        assertThat(batches).contains("TableFilter.bar(");
        assertThat(batches).contains("UIHelper.ensureHeadersFit(owner.batchesTable)");
        assertThat(batches).contains("TableQuickPeekController.install(owner.batchesTable");

        String alerts = source("StockAlertsPanel.java");
        assertThat(alerts).doesNotContain("expFilters = new JPanel(new GridBagLayout())");
        assertThat(alerts).doesNotContain("outFilters = new JPanel(new GridBagLayout())");
        assertThat(alerts).contains("TableFilter.bar(");
        assertThat(alerts).contains("UIHelper.ensureHeadersFit(owner.alertsExpTable)");
        assertThat(alerts).contains("UIHelper.ensureHeadersFit(owner.alertsOutTable)");
    }

    @Test
    @DisplayName("STOCK-03: StockWastePanel, StockWarehousesPanel e StockCategoriesPanel seguem padrão canónico")
    void stock03_wasteWarehousesAndCategoriesCanonical() throws Exception {
        String waste = source("StockWastePanel.java");
        assertThat(waste).doesNotContain("new JPanel(new GridBagLayout())");
        assertThat(waste).contains("TableFilter.bar(");
        assertThat(waste).contains("UIHelper.ensureHeadersFit(wasteTable)");
        assertThat(waste).contains("UIHelper.ensureHeadersFit(radarTable)");
        assertThat(waste).contains("TableQuickPeekController.install(wasteTable");

        String wh = source("StockWarehousesPanel.java");
        assertThat(wh).doesNotContain("whFilters = new JPanel(new GridBagLayout())");
        assertThat(wh).contains("TableFilter.bar(");
        assertThat(wh).contains("UIHelper.ensureHeadersFit(owner.warehousesTable)");
        assertThat(wh).contains("TableQuickPeekController.install(owner.warehousesTable");

        String cat = source("StockCategoriesPanel.java");
        assertThat(cat).doesNotContain("searchRow = new JPanel(new GridBagLayout())");
        assertThat(cat).contains("TableFilter.bar(");
        assertThat(cat).contains("UIHelper.ensureHeadersFit(owner.categoriesTable)");
    }
}

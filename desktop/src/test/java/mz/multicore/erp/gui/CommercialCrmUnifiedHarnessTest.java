package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness COMM-01..03:
 * Valida a conformidade canónica da Gestão Comercial, CRM & Histórico POS:
 * - Filtros em linha única horizontal via TableFilter.bar (sem GridBagLayout em 2 linhas)
 * - Calibração de larguras de coluna com UIHelper.ensureHeadersFit
 * - Instalação de Quick Peek silencioso com tecla Espaço
 */
class CommercialCrmUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("COMM-01: ClientesPanel e PromotionsPanel sem GridBagLayout em filtros e com ensureHeadersFit")
    void comm01_clientesAndPromotionsCanonical() throws Exception {
        String clientes = source("ClientesPanel.java");
        assertThat(clientes).doesNotContain("searchRow = new JPanel(new GridBagLayout())");
        assertThat(clientes).contains("TableFilter.bar(");
        assertThat(clientes).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(clientes).contains("TableQuickPeekController.install(table");

        String promo = source("PromotionsPanel.java");
        assertThat(promo).doesNotContain("promoFilters = new JPanel(new GridBagLayout())");
        assertThat(promo).contains("TableFilter.bar(");
        assertThat(promo).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(promo).contains("TableQuickPeekController.install(table");
    }

    @Test
    @DisplayName("COMM-02: Painéis comerciais secundários (OutstandingAccounts, Notes, Movements) canónicos")
    void comm02_commercialSubPanelsCanonical() throws Exception {
        String accounts = source("commercial/OutstandingAccountsPanel.java");
        assertThat(accounts).doesNotContain("new JPanel(new GridBagLayout())");
        assertThat(accounts).contains("TableFilter.bar(");
        assertThat(accounts).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(accounts).contains("TableQuickPeekController.install(table");

        String notes = source("commercial/CommercialNotesPanel.java");
        assertThat(notes).doesNotContain("new JPanel(new GridBagLayout())");
        assertThat(notes).contains("TableFilter.bar(");
        assertThat(notes).contains("UIHelper.ensureHeadersFit(creditTable)");
        assertThat(notes).contains("UIHelper.ensureHeadersFit(debitTable)");

        String movements = source("commercial/CommercialMovementsPanel.java");
        assertThat(movements).doesNotContain("new JPanel(new GridBagLayout())");
        assertThat(movements).contains("TableFilter.bar(");
        assertThat(movements).contains("UIHelper.ensureHeadersFit(table)");
    }

    @Test
    @DisplayName("COMM-03: PosSalesHistoryPanel sem GridBagLayout em filtros e com ensureHeadersFit")
    void comm03_posSalesHistoryCanonical() throws Exception {
        String posHistory = source("PosSalesHistoryPanel.java");
        assertThat(posHistory).doesNotContain("shBar = new JPanel(new GridBagLayout())");
        assertThat(posHistory).contains("TableFilter.bar(");
        assertThat(posHistory).contains("UIHelper.ensureHeadersFit(owner.salesHistoryTable)");
    }
}

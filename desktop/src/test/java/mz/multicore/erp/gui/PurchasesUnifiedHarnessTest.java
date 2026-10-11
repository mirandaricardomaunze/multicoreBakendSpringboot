package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness PURCH-01..03:
 * Valida a conformidade canónica do módulo de Compras e Fornecedores:
 * - Filtros em linha única horizontal via TableFilter.bar (sem GridBagLayout em 2 linhas)
 * - Ausência de reticências proibidas nos botões
 * - Ausência de cabeçalhos/títulos duplicados fora do card
 * - Instalação de Quick Peek silencioso com tecla Espaço
 * - Calibração de larguras de coluna com UIHelper.ensureHeadersFit
 */
class PurchasesUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("PURCH-01: PurchaseOrdersPanel segue padrão de card único, sem GridBagLayout em filtros e com Quick Peek")
    void purch01_purchaseOrdersCanonical() throws Exception {
        String s = source("PurchaseOrdersPanel.java");

        // Card único e TableFilter.bar
        assertThat(s).contains("TableFilter.bar(");
        assertThat(s).contains("UIHelper.tableCardTop(");
        assertThat(s).contains("TableQuickPeekController.install(poListTable");
        assertThat(s).contains("UIHelper.ensureHeadersFit(poListTable)");

        // Proibição de GridBagLayout em filtros
        assertThat(s).doesNotContain("poFilters = new JPanel(new GridBagLayout())");

        // Proibição de reticências
        assertThat(s).doesNotContain("\"Nova Encomenda…\"");
        assertThat(s).doesNotContain("\"Receber Parcial…\"");
    }

    @Test
    @DisplayName("PURCH-02: PurchasePayablesPanel sem cabeçalho duplicado fora do card, filtros em linha única e Quick Peek")
    void purch02_purchasePayablesCanonical() throws Exception {
        String s = source("PurchasePayablesPanel.java");

        // Sem duplicação de cabeçalho fora do card
        assertThat(s).doesNotContain("tab.add(header");
        assertThat(s).contains("UIHelper.tableCardTop(");

        // Filtros e Quick Peek
        assertThat(s).contains("TableFilter.bar(");
        assertThat(s).doesNotContain("payFilters = new JPanel(new GridBagLayout())");
        assertThat(s).contains("TableQuickPeekController.install(owner.payablesTable");
        assertThat(s).contains("UIHelper.ensureHeadersFit(owner.payablesTable)");
    }

    @Test
    @DisplayName("PURCH-03: PurchaseSuppliersPanel segue padrão canónico com filtros em linha única e Quick Peek")
    void purch03_purchaseSuppliersCanonical() throws Exception {
        String s = source("PurchaseSuppliersPanel.java");

        assertThat(s).contains("TableFilter.bar(");
        assertThat(s).doesNotContain("supFilters = new JPanel(new GridBagLayout())");
        assertThat(s).contains("TableQuickPeekController.install(owner.suppliersTable");
        assertThat(s).contains("UIHelper.ensureHeadersFit(owner.suppliersTable)");
    }
}

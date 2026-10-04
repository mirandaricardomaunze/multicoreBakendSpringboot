package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TableActionPlacementHarnessTest {

    @Test
    void toolbarCanonico_alinhaFiltrosAEsquerdaEAccoesADireita() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField search = TableFilter.searchField("Pesquisar…");
            JComboBox<String> status = TableFilter.combo("Todos", "Activo");
            ModernButton refresh = UIHelper.createRefreshButton(() -> { });
            ModernButton create = UIHelper.createPrimaryButton("Novo");

            JPanel toolbar = UIHelper.filterBar(
                    new JComponent[]{search, TableFilter.label("Estado:"), status},
                    new JComponent[]{refresh, create});

            assertThat(toolbar.getLayout()).isInstanceOf(BorderLayout.class);
            BorderLayout layout = (BorderLayout) toolbar.getLayout();
            assertThat(layout.getLayoutComponent(BorderLayout.WEST)).isInstanceOf(JPanel.class);
            assertThat(layout.getLayoutComponent(BorderLayout.EAST)).isInstanceOf(JPanel.class);
            assertControlHeight(search);
            assertControlHeight(status);
            assertControlHeight(refresh);
            assertControlHeight(create);
        });
    }

    @Test
    void topoDeCardCanonico_separaCabecalhoDeFiltros() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField search = TableFilter.searchField("Pesquisar…");
            JPanel filters = TableFilter.bar(search);
            ModernButton refresh = UIHelper.createRefreshButton(() -> { });
            ModernButton create = UIHelper.createPrimaryButton("Novo");

            JPanel top = UIHelper.tableCardTop("Registos", filters, refresh, create);

            assertThat(top.getLayout()).isInstanceOf(BorderLayout.class);
            BorderLayout layout = (BorderLayout) top.getLayout();
            assertThat(layout.getLayoutComponent(BorderLayout.NORTH)).isInstanceOf(JPanel.class);
            assertThat(layout.getLayoutComponent(BorderLayout.CENTER)).isSameAs(filters);
            JPanel header = (JPanel) layout.getLayoutComponent(BorderLayout.NORTH);
            assertThat(((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.EAST))
                    .isInstanceOf(JPanel.class);
            assertControlHeight(search);
            assertControlHeight(refresh);
            assertControlHeight(create);
        });
    }

    @Test
    void listasMigradas_naoReintroduzemRodapesComBotoes() throws Exception {
        Path gui = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        Map<String, List<String>> forbiddenByFile = new LinkedHashMap<>();
        forbiddenByFile.put("CommercialInvoicesView.java", List.of(
                "formCard.add(addLineActionRow, gbc)",
                "listSouth.add(footer, BorderLayout.SOUTH)",
                "listCard.add(footer, BorderLayout.SOUTH)"));
        forbiddenByFile.put("CommercialOrdersView.java", List.of(
                "listCard.add(footer, BorderLayout.SOUTH)"));
        forbiddenByFile.put("ComprasPanel.java", List.of(
                "historyCard.add(actionRow, BorderLayout.SOUTH)"));
        forbiddenByFile.put("ConfigPanel.java", List.of(
                "archiveCard.add(actionRow, BorderLayout.SOUTH)",
                "card.add(btnPanel, BorderLayout.SOUTH)"));
        forbiddenByFile.put("NotificationsPanel.java", List.of(
                "card.add(footer, BorderLayout.SOUTH)"));
        forbiddenByFile.put("accounting/AccountingPanel.java", List.of(
                "card.add(buttons(refresh, create, seed), BorderLayout.SOUTH)",
                "footerBar.add(buttons(create), BorderLayout.EAST)"));
        forbiddenByFile.put("commercial/ReceiptsPanel.java", List.of(
                "card.add(actions, BorderLayout.SOUTH)"));
        forbiddenByFile.put("commercial/DeliveryGuidesPanel.java", List.of(
                "card.add(actions, BorderLayout.SOUTH)"));
        forbiddenByFile.put("commercial/QuotationsPanel.java", List.of(
                "card.add(actions, BorderLayout.SOUTH)"));
        forbiddenByFile.put("commercial/CommercialNotesPanel.java", List.of(
                "tablePanel.add(buttons, BorderLayout.SOUTH)"));
        forbiddenByFile.put("StockTransferActions.java", List.of(
                "linesWrap.add(lineButtons, BorderLayout.SOUTH)"));

        for (Map.Entry<String, List<String>> entry : forbiddenByFile.entrySet()) {
            String source = Files.readString(gui.resolve(entry.getKey()));
            for (String forbidden : entry.getValue()) {
                assertThat(source)
                        .as("%s deve manter as acções no topo da tabela", entry.getKey())
                        .doesNotContain(forbidden);
            }
        }
    }

    @Test
    void dialogoDeBackup_mantemVerificacaoNoTopoEEstadoNoRodape() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui",
                "components", "DatabaseBackupDialog.java");
        String source = Files.readString(sourcePath);

        assertThat(source).contains("tableCard.add(UIHelper.actionsBar(verifyBtn), BorderLayout.NORTH)");
        assertThat(source).contains("tableCard.add(tableActions, BorderLayout.SOUTH)");
        assertThat(source).doesNotContain("tableActions.add(verifyBtn, BorderLayout.EAST)");
    }

    @Test
    void faturacao_mantemAccoesEFiltrosNoTopoDoMesmoCard() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui",
                "CommercialInvoicesView.java");
        String source = Files.readString(sourcePath);

        assertThat(source).contains("cardHeader.add(UIHelper.actionsBar(refreshBtn, moreBtn, issueBtn)");
        assertThat(source).contains("cardTop.add(cardHeader, BorderLayout.NORTH)");
        assertThat(source).contains("cardTop.add(invFilters, BorderLayout.CENTER)");
        assertThat(source).contains("listCard.add(cardTop, BorderLayout.NORTH)");
        assertThat(source).doesNotContain("panel.add(headerBar, BorderLayout.NORTH)");
        assertThat(source).doesNotContain("new JComponent[]{moreBtn, cancelInvoiceBtn, payInvoiceBtn}");
    }

    @Test
    void modulosOperacionais_usamTopoCanonicoDentroDoCard() throws Exception {
        Path gui = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        List<String> migrated = List.of(
                "ClientesPanel.java",
                "ApprovalsPanel.java",
                "CommercialOrdersView.java",
                "ComprasPanel.java",
                "CRMPanel.java",
                "FiscalPanel.java",
                "HRContractsPanel.java",
                "HRDeductionsPanel.java",
                "HRExpensesPanel.java",
                "HRLiabilitiesPanel.java",
                "HRPanel.java",
                "HRTerminationsPanel.java",
                "HRTimeSheetPanel.java",
                "HRVacationsPanel.java",
                "NotificationsPanel.java",
                "PlataformaPanel.java",
                "PromotionsPanel.java",
                "PurchasePayablesPanel.java",
                "PurchaseReorderPanel.java",
                "PurchaseSuppliersPanel.java",
                "StockAlertsPanel.java",
                "StockBatchesPanel.java",
                "StockCategoriesPanel.java",
                "StockPanel.java",
                "StockWarehousesPanel.java",
                "accounting/AccountingPanel.java",
                "commercial/CommercialMovementsPanel.java",
                "commercial/CommercialNotesPanel.java",
                "commercial/DeliveryGuidesPanel.java",
                "commercial/OutstandingAccountsPanel.java",
                "commercial/QuotationsPanel.java",
                "commercial/ReceiptsPanel.java",
                "inventory/PhysicalInventoryPanel.java",
                "users/UserManagementPanel.java");

        for (String relative : migrated) {
            assertThat(Files.readString(gui.resolve(relative)))
                    .as("%s deve manter título, acções e filtros no topo do card", relative)
                    .contains("UIHelper.tableCardTop(");
        }
    }

    private static void assertControlHeight(Component component) {
        assertThat(component.getPreferredSize().height).isEqualTo(UIHelper.FORM_CONTROL_HEIGHT);
    }
}

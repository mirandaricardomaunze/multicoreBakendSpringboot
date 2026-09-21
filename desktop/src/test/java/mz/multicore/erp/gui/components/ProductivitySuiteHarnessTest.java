package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.DashboardPanel;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de testes automatizados para validar o SPEC docs/PRODUTIVIDADE_TOTAL_SPEC.md
 * e HARNESS docs/PRODUTIVIDADE_TOTAL_HARNESS.md.
 */
class ProductivitySuiteHarnessTest {

    @Test
    void pt01_exportsTableToCsvWithProperEscapingAndColumns() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Código", "Nome", "Preço (MT)", "Observação"}, 0);
        model.addRow(new Object[]{"SKU-001", "Arroz Basmati, 5kg", "750.00", "Promoção; Especial"});
        model.addRow(new Object[]{"SKU-002", "Óleo \"Girassol\" 1L", "120.50", "Sem lote"});

        JTable table = new JTable(model);
        String csv = TableCsvExporter.toCsvString(table);

        assertThat(csv).isNotNull();
        assertThat(csv).contains("Código;Nome;Preço (MT);Observação");
        // Teste de escape de aspas e ponto e vírgula
        assertThat(csv).contains("\"Arroz Basmati, 5kg\"");
        assertThat(csv).contains("\"Promoção; Especial\"");
        assertThat(csv).contains("\"Óleo \"\"Girassol\"\" 1L\"");
    }

    @Test
    void pt02_tableContextMenuIncludesCsvExportOption() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nome"}, 0);
            model.addRow(new Object[]{"1", "Produto Teste"});
            JTable table = new JTable(model);
            JScrollPane scroll = new JScrollPane(table);

            TableContextMenu.install(scroll);
            assertTrue(Boolean.TRUE.equals(scroll.getClientProperty("tableContextMenu.installed")));

            JPopupMenu popup = TableContextMenu.buildMenu(table, scroll.getVerticalScrollBar(), 0, 0);
            assertThat(popup).isNotNull();

            boolean hasCsvItem = false;
            for (int i = 0; i < popup.getComponentCount(); i++) {
                if (popup.getComponent(i) instanceof JMenuItem item && item.getText().contains("CSV")) {
                    hasCsvItem = true;
                    assertTrue(item.isEnabled());
                    break;
                }
            }
            assertTrue(hasCsvItem, "O menu de contexto deve incluir a opção de exportação CSV");
        });
    }

    @Test
    void pt03_shelfLabelsDialogRendersFormattedProductLabels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProductDTO p = new ProductDTO(
                    101L, "SKU-01", "REF-X", "BAR-789012", "Açúcar Branco 1kg",
                    BigDecimal.valueOf(85.00), BigDecimal.valueOf(70.00), BigDecimal.valueOf(5),
                    BigDecimal.valueOf(80.00), BigDecimal.valueOf(10), 1, "UNIT",
                    true, 1L, "ALIMENTAR", 1L, BigDecimal.valueOf(16), "IVA 16%",
                    "Açúcar de primeira", null, BigDecimal.ONE, BigDecimal.ONE
            );

            var labelCard = ShelfLabelsDialog.createSingleLabelCard(p);
            assertThat(labelCard).isNotNull();
            assertEquals(240, labelCard.getPreferredSize().width);
            assertEquals(130, labelCard.getPreferredSize().height);

            ShelfLabelsDialog dialog = new ShelfLabelsDialog(null, List.of(p));
            assertThat(dialog.getTitle()).contains("Etiquetas");
            dialog.dispose();
        });
    }

    @Test
    void pt04_topProductsWidgetRendersRankedItems() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TopProductsWidget widget = new TopProductsWidget();
            widget.setProducts(List.of(
                    new TopProductsWidget.RankedProduct("Arroz Basmati", BigDecimal.valueOf(15000), 20, 60.0),
                    new TopProductsWidget.RankedProduct("Óleo Girassol", BigDecimal.valueOf(10000), 80, 40.0)
            ));

            assertThat(widget.getProducts()).hasSize(2);
            assertEquals("Arroz Basmati", widget.getProducts().get(0).name());
            assertEquals(60.0, widget.getProducts().get(0).percentage());
        });
    }

    @Test
    void pt05_recentActivityWidgetRendersTimelineEntries() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecentActivityWidget widget = new RecentActivityWidget();
            widget.setActivities(List.of(
                    new RecentActivityWidget.ActivityEntry(
                            "act_1", "Fatura FT-2026/01", "Cliente: Teste · 1.500,00 MT", "Hoje",
                            "fas-file-invoice-dollar", UIHelper.ACCENT_BLUE
                    ),
                    new RecentActivityWidget.ActivityEntry(
                            "act_2", "Venda POS #12", "Balcão · 450,00 MT", "Hoje",
                            "fas-cash-register", UIHelper.APPROVED_GREEN
                    )
            ));

            assertThat(widget.getActivities()).hasSize(2);
            assertEquals("Fatura FT-2026/01", widget.getActivities().get(0).title());
        });
    }

    @Test
    void pt06_dashboardPeriodFilterEnumValues() {
        DashboardPanel.PeriodFilter[] values = DashboardPanel.PeriodFilter.values();
        assertThat(values).contains(
                DashboardPanel.PeriodFilter.HOJE,
                DashboardPanel.PeriodFilter.ESTA_SEMANA,
                DashboardPanel.PeriodFilter.ESTE_MES,
                DashboardPanel.PeriodFilter.ESTE_ANO,
                DashboardPanel.PeriodFilter.TODOS
        );
        assertEquals("Hoje", DashboardPanel.PeriodFilter.HOJE.getLabel());
        assertEquals("Esta Semana", DashboardPanel.PeriodFilter.ESTA_SEMANA.getLabel());
        assertEquals("Este Mês", DashboardPanel.PeriodFilter.ESTE_MES.getLabel());
        assertEquals("Este Ano", DashboardPanel.PeriodFilter.ESTE_ANO.getLabel());
        assertEquals("Todo o Período", DashboardPanel.PeriodFilter.TODOS.getLabel());
    }
}

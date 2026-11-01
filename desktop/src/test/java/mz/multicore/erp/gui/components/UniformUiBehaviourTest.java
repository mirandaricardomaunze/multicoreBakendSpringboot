package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Component;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/UI_ORGANIZACAO_NAVEGACAO_SPEC.md. */
class UniformUiBehaviourTest {

    @Test
    @DisplayName("Filtros que não cabem encolhem e ficam numa linha")
    void filtersShrinkToFit() {
        int[] preferred = {160, 50, 145, 50, 140, 130};
        boolean[] flexible = {true, false, true, false, true, false};
        boolean[] none = new boolean[6];

        int[] widths = SingleRowFilterLayout.fit(preferred, flexible, none, 620, 8);

        int used = 8 * 5;
        for (int width : widths) used += width;
        assertThat(used).isLessThanOrEqualTo(620);
        assertThat(widths[1]).isEqualTo(50);
        assertThat(widths[5]).as("data").isEqualTo(130);
    }

    @Test
    @DisplayName("Filtros que cabem não mudam, e nenhum desce abaixo do mínimo")
    void filtersKeepLimits() {
        int[] preferred = {160, 145};
        boolean[] flexible = {true, true};

        boolean[] none = new boolean[2];

        assertThat(SingleRowFilterLayout.fit(preferred, flexible, none, 400, 8)).containsExactly(160, 145);
        assertThat(SingleRowFilterLayout.fit(preferred, flexible, none, 100, 8)).containsExactly(90, 90);
    }

    @Test
    @DisplayName("Sem espaço, sai a etiqueta antes de cortar o selector; só as necessárias")
    void redundantLabelsLeaveFirst() {
        int[] preferred = {50, 145, 50, 145};
        boolean[] flexible = {false, true, false, true};
        boolean[] redundant = {true, false, true, false};

        assertThat(SingleRowFilterLayout.fit(preferred, flexible, redundant, 414, 8))
                .containsExactly(50, 145, 50, 145);
        assertThat(SingleRowFilterLayout.fit(preferred, flexible, redundant, 360, 8))
                .containsExactly(0, 145, 50, 145);
    }

    @Test
    @DisplayName("A aba escolhida por outro ecrã fica visível")
    void selectedTabIsRevealed() throws Exception {
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            UIHelper.styleTabbedPaneMulticore(tabs);
            for (String title : List.of("Cotações", "Faturação", "Pedidos & Separação", "Recibos", "Guias",
                    "Notas de Crédito", "Notas de Débito", "Contas Correntes", "Promoções", "Movimentos")) {
                tabs.addTab(title, new JPanel());
            }
            tabs.setSize(400, 300);
            tabs.doLayout();
            tabs.setSelectedIndex(9);
            holder[0] = tabs;
        });
        SwingUtilities.invokeAndWait(() -> { });

        JViewport viewport = null;
        for (Component child : holder[0].getComponents()) {
            if (child instanceof JViewport found) viewport = found;
        }
        assertThat(viewport).isNotNull();
        assertThat(viewport.getViewPosition().x).isPositive();
    }

    @Test
    @DisplayName("O campo de data mostra sempre o formato")
    void dateFieldShowsFormat() {
        assertThat(new DateField().getClientProperty("JTextField.placeholderText")).isEqualTo("aaaa-mm-dd");
    }

    @Test
    @DisplayName("Tabela sem dados não sugere mexer em filtros")
    void emptyTableHasNoFilterHint() {
        JTable table = new JTable(new DefaultTableModel(new Object[]{"A"}, 0));
        table.setRowSorter(new TableRowSorter<TableModel>(table.getModel()));

        assertThat(TableEmptyState.resolveSubtext(table)).isEmpty();
    }

    @Test
    @DisplayName("Filtro que esconde todas as linhas mostra a dica")
    void hiddenRowsShowFilterHint() {
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{"x"}}, new Object[]{"A"}));
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter("nada"));

        assertThat(TableEmptyState.resolveSubtext(table)).contains("filtro");
    }

    @Test
    @DisplayName("Pesquisa continua a ser um campo flexível, a data não")
    void flexibilityByType() {
        JPanel bar = TableFilter.bar(new JTextField(), new DateField());

        assertThat(bar.getLayout()).isInstanceOf(SingleRowFilterLayout.class);
    }
}

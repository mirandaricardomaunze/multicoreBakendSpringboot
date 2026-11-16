package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness da consistência das tabelas (docs/TABELAS_CONSISTENCIA_SPEC.md). */
class TableConsistencyHarnessTest {

    @Test
    @DisplayName("TC-01: um só botão verde cheio; os outros de contorno; nenhum bloco vermelho")
    void tc01_buttonHierarchy() {
        ModernButton refresh = UIHelper.createRefreshButton(() -> { });
        ModernButton manage = UIHelper.createPrimaryButton("Gerir Cotação");
        ModernButton other = UIHelper.createSuccessButton("Duplicar");
        ModernButton create = UIHelper.createSuccessButton("Nova Cotação");
        ModernButton cancel = UIHelper.createDangerButton("Anular Recibo");

        JPanel top = UIHelper.tableCardTop("Cotações", new JPanel(), refresh, manage, other, cancel, create);
        List<ModernButton> buttons = buttons(((BorderLayout) top.getLayout()).getLayoutComponent(BorderLayout.EAST));

        assertThat(buttons).as("nada se esconde nem muda de ordem").containsExactly(refresh, manage, other, cancel, create);
        assertThat(buttons.stream().filter(b -> !ButtonStyles.isOutline(b))).as("só a acção principal tem cor cheia").containsExactly(create);
        assertThat(ButtonStyles.isOutline(cancel)).isTrue();
        assertThat(cancel.getForeground()).as("perigo no texto, não no fundo").isEqualTo(UIHelper.REJECTED_RED_HOVER);
        assertThat(ButtonStyles.isOutline(other)).as("verde que não é o último também fica sóbrio").isTrue();
    }

    @Test
    @DisplayName("TC-02: \"Remover linha\" dos editores continua à vista")
    void tc02_editorRemoveStaysVisible() {
        ModernButton add = UIHelper.createSuccessButton("Adicionar linha");
        ModernButton remove = UIHelper.createDangerButton("Remover linha");
        JPanel top = UIHelper.tableCardTop("Linhas da Fatura", null, add, remove);
        List<ModernButton> buttons = buttons(((BorderLayout) top.getLayout()).getLayoutComponent(BorderLayout.EAST));
        assertThat(buttons).containsExactly(add, remove);
        assertThat(ButtonStyles.isOutline(remove)).isTrue();
    }

    @Test
    @DisplayName("TC-03: o mesmo estado tem a mesma cor nas duas tabelas; estado é texto, não etiqueta")
    void tc03_sharedStatusVocabulary() {
        assertThat(StatusTone.toneOf("Cancelado")).isEqualTo(StatusTone.Tone.BAD);
        assertThat(StatusTone.toneOf("Paga")).isEqualTo(StatusTone.Tone.GOOD);
        assertThat(StatusTone.toneOf("Faturado")).isEqualTo(StatusTone.Tone.GOOD);
        assertThat(StatusTone.toneOf("Aguarda Decisão")).isEqualTo(StatusTone.Tone.WARN);
        assertThat(StatusTone.toneOf("Em Trânsito")).isEqualTo(StatusTone.Tone.INFO);
        assertThat(StatusTone.toneOf("Consumidor Final")).isEqualTo(StatusTone.Tone.NEUTRAL);

        for (String state : List.of("Paga", "Faturado", "Cancelado", "Pendente")) {
            DefaultTableModel model = new DefaultTableModel(new Object[]{"Estado"}, 0);
            model.addRow(new Object[]{state});
            JTable generic = new JTable(model);
            UIHelper.styleTable(generic);
            Color genericColour = generic.prepareRenderer(generic.getCellRenderer(0, 0), 0, 0).getForeground();
            Color statusColour = TableCellRenderers.status().getTableCellRendererComponent(generic, state, false, false, 0, 0).getForeground();
            assertThat(genericColour).as(state).isEqualTo(statusColour).isEqualTo(StatusTone.colorOf(StatusTone.toneOf(state)));
        }
    }

    @Test
    @DisplayName("TC-04: rodapé igual em todas as tabelas, também quando vazias")
    void tc04_pagerText() {
        assertThat(ClientTablePagination.recordsText(0)).isEqualTo("0 registos");
        assertThat(ClientTablePagination.recordsText(1)).isEqualTo("1 registo");
        assertThat(ClientTablePagination.recordsText(12)).isEqualTo("12 registos");
        assertThat(ClientTablePagination.pageText(0, 0)).isEqualTo("Página  1  de  1");
    }

    @Test
    @DisplayName("TC-05: o botão de espreitar fica no fim dos filtros, onde quer que o ecrã o ponha")
    void tc05_peekToggleAtEndOfFilters() {
        JButton fromActions = peek();
        JPanel filters = new JPanel();
        JPanel top = UIHelper.tableCardTop("Clientes", filters, UIHelper.createRefreshButton(() -> { }), fromActions);
        Component center = ((BorderLayout) top.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        Component east = ((BorderLayout) ((Container) center).getLayout()).getLayoutComponent(BorderLayout.EAST);
        assertThat(javax.swing.SwingUtilities.isDescendingFrom(fromActions, east)).as("no fim dos filtros").isTrue();
        assertThat(fromActions.getPreferredSize().height).as("mesma altura de um campo").isEqualTo(UIHelper.FORM_CONTROL_HEIGHT);
        assertThat(buttons(((BorderLayout) top.getLayout()).getLayoutComponent(BorderLayout.EAST))).doesNotContain((ModernButton) fromActions);

        JButton insideFilters = peek();
        JPanel bar = new JPanel();
        bar.add(new JLabel("Pesquisa"));
        bar.add(insideFilters);
        JPanel top2 = UIHelper.tableCardTop("Clientes", bar);
        Component center2 = ((BorderLayout) top2.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        Component east2 = ((BorderLayout) ((Container) center2).getLayout()).getLayoutComponent(BorderLayout.EAST);
        assertThat(javax.swing.SwingUtilities.isDescendingFrom(insideFilters, east2)).isTrue();
        assertThat(insideFilters.getParent()).isNotSameAs(bar);
        assertThat(ButtonStyles.isOutline((ModernButton) insideFilters)).isTrue();
    }

    private static JButton peek() {
        ModernButton peek = UIHelper.createSecondaryButton("");
        peek.setIcon(UIHelper.icon("fas-columns", 12));
        peek.setToolTipText("Espreitar detalhes da linha seleccionada (Espaço)");
        return peek;
    }

    private static List<ModernButton> buttons(Component root) {
        List<ModernButton> found = new ArrayList<>();
        if (root instanceof ModernButton button) found.add(button);
        if (root instanceof Container container && !(root instanceof JComponent c && c instanceof ModernButton)) {
            for (Component child : container.getComponents()) found.addAll(buttons(child));
        }
        return found;
    }
}

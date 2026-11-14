package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness dos atalhos, do estado vazio com acção e do tema escuro (docs/TABELAS_CONSISTENCIA_SPEC.md). */
class TableShortcutsAndEmptyStateHarnessTest {

    @Test
    @DisplayName("TC-06: Ctrl+N corre a acção principal, F5 actualiza, Ctrl+F vai à pesquisa; tooltips dizem o atalho")
    void tc06_sameShortcutsOnEveryHeader() {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger refreshed = new AtomicInteger();
        ModernButton refresh = UIHelper.createRefreshButton(refreshed::incrementAndGet);
        ModernButton create = UIHelper.createSuccessButton("Nova Cotação");
        create.addActionListener(e -> created.incrementAndGet());
        JTextField search = TableFilter.searchField("Pesquisar…");
        JPanel filters = TableFilter.bar(search, TableFilter.combo("Todos os estados", "Pendente"));

        JComponent top = UIHelper.tableCardTop("Cotações", filters, refresh, create);

        Action newAction = action(top, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        Action refreshAction = action(top, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        Action searchAction = action(top, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        assertThat(newAction).isNotNull();
        assertThat(refreshAction).isNotNull();
        assertThat(searchAction).isNotNull();
        assertThat(newAction.isEnabled()).as("ecrã escondido não responde ao atalho").isFalse();

        newAction.actionPerformed(new ActionEvent(top, ActionEvent.ACTION_PERFORMED, "n"));
        refreshAction.actionPerformed(new ActionEvent(top, ActionEvent.ACTION_PERFORMED, "f5"));
        assertThat(created).hasValue(1);
        assertThat(refreshed).hasValue(1);
        assertThat(create.getToolTipText()).contains("Ctrl+N");
        assertThat(refresh.getToolTipText()).contains("(F5)");
        assertThat(search.getToolTipText()).contains("Ctrl+F");
    }

    @Test
    @DisplayName("TC-07: tabela vazia oferece a acção principal; filtros que escondem tudo oferecem \"Limpar filtros\"")
    void tc07_emptyStateOffersTheNextStep() {
        AtomicInteger created = new AtomicInteger();
        ModernButton create = UIHelper.createSuccessButton("Nova Cotação");
        create.addActionListener(e -> created.incrementAndGet());
        JTextField search = TableFilter.searchField("Pesquisar…");
        JComboBox<String> state = TableFilter.combo("Todos os estados", "Pendente");
        JPanel filters = TableFilter.bar(search, state);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Nº"}, 0);
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        TableEmptyState.install(scroll);
        JPanel card = new JPanel(new BorderLayout());
        card.add(scroll, BorderLayout.CENTER);
        card.add(UIHelper.tableCardTop("Cotações", filters, UIHelper.createRefreshButton(() -> { }), create), BorderLayout.NORTH);

        assertThat(TableEmptyState.resolveActionLabel(table)).isEqualTo("Nova Cotação");
        TableEmptyState.runAction(table);
        assertThat(created).hasValue(1);

        create.setEnabled(false);
        assertThat(TableEmptyState.resolveActionLabel(table)).as("acção desactivada não é oferecida").isNull();

        model.addRow(new Object[]{"CT-1"});
        search.setText("xyz");
        state.setSelectedIndex(1);
        assertThat(TableEmptyState.resolveActionLabel(table)).isEqualTo(TableEmptyState.CLEAR_FILTERS);
        TableEmptyState.runAction(table);
        assertThat(search.getText()).isEmpty();
        assertThat(state.getSelectedIndex()).isZero();
    }

    @Test
    @DisplayName("TC-08: no tema escuro os botões de contorno continuam legíveis")
    void tc08_outlineReadableInDarkTheme() {
        try {
            UiThemeAndWindowSupport.applyTheme(Theme.DARK);
            ModernButton refresh = UIHelper.createRefreshButton(() -> { });
            ModernButton cancel = UIHelper.createDangerButton("Anular Recibo");
            UIHelper.tableCardTop("Recibos", new JPanel(), refresh, cancel);

            assertThat(refresh.getBackground()).isEqualTo(UIHelper.BG_CARD);
            assertThat(UIHelper.contrastRatio(refresh.getBackground(), refresh.getForeground())).isGreaterThanOrEqualTo(4.5);
            assertThat(UIHelper.contrastRatio(cancel.getBackground(), cancel.getForeground())).isGreaterThanOrEqualTo(3.0);
        } finally {
            UiThemeAndWindowSupport.applyTheme(Theme.LIGHT);
        }
    }

    private static Action action(JComponent top, KeyStroke key) {
        Object name = top.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key);
        return name == null ? null : top.getActionMap().get(name);
    }
}

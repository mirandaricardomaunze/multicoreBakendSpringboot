package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import static org.assertj.core.api.Assertions.assertThat;

class RowDetailsInspectorTest {
    @Test
    void installIsIdempotentAndWiresEnter() {
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{"Ana"}}, new Object[]{"Nome"}));
        int before = table.getMouseListeners().length;

        RowDetailsInspector.install(table);
        RowDetailsInspector.install(table);

        assertThat(table.getMouseListeners()).hasSize(before + 1);
        Object binding = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .get(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0));
        assertThat(binding).isEqualTo(RowDetailsInspector.OPEN_ACTION);
        assertThat(table.getActionMap().get(RowDetailsInspector.OPEN_ACTION)).isNotNull();
    }

    @Test
    void valueAndHiddenColumnRulesAreCentralised() {
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{1L, null}}, new Object[]{"ID", "Nome"}));
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        assertThat(RecordDetailsDialog.isHidden(table, 0)).isTrue();
        assertThat(RecordDetailsDialog.isHidden(table, 1)).isFalse();
        assertThat(RecordDetailsDialog.valueText(null)).isEmpty();
        assertThat(RecordDetailsDialog.valueText(25)).isEqualTo("25");
    }
}

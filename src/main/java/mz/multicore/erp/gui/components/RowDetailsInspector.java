package mz.multicore.erp.gui.components;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Instala uma única porta DRY para abrir detalhes: duplo clique ou Enter. */
public final class RowDetailsInspector {
    static final String INSTALLED = "rowDetailsInspector.installed";
    static final String OPEN_ACTION = "rowDetailsInspector.open";

    private RowDetailsInspector() {}

    public static void install(JTable table) {
        if (table == null || Boolean.TRUE.equals(table.getClientProperty(INSTALLED))) return;
        table.putClientProperty(INSTALLED, Boolean.TRUE);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) open(table);
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), OPEN_ACTION);
        table.getActionMap().put(OPEN_ACTION, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { open(table); }
        });
    }

    public static void open(JTable table) {
        if (table == null || table.getSelectedRow() < 0
                || Boolean.TRUE.equals(table.getClientProperty("noRowInspector"))) return;
        RecordDetailsDialog.show(table);
    }
}

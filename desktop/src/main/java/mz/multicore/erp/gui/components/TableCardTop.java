package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;

/**
 * Header of a table card on one row: title, filters, then actions on the right.
 * The title hides itself when the enclosing tab already shows it (the top bar path names it too),
 * so the table does not lose a whole row to a repeated name.
 */
final class TableCardTop {

    private TableCardTop() {}

    static JPanel build(String title, JComponent filters, JComponent... actions) {
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel heading = UIHelper.createSubheading(title);
        top.add(heading, filters != null ? BorderLayout.WEST : BorderLayout.CENTER);
        if (filters != null) top.add(filters, BorderLayout.CENTER);
        if (actions != null && actions.length > 0) top.add(UIHelper.actionsBar(actions), BorderLayout.EAST);

        heading.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED)) != 0) {
                heading.setVisible(!TabTitles.repeatsTab(heading, title));
            }
        });
        return top;
    }
}

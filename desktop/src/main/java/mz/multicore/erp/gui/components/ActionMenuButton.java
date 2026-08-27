package mz.multicore.erp.gui.components;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

/** Botão canónico para agrupar poucas acções secundárias relacionadas. */
public class ActionMenuButton extends ModernButton {

    private static final int MAX_ACTIONS = 5;
    private final JPopupMenu popup = new JPopupMenu();

    ActionMenuButton(String text) {
        super(text);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("O menu de acções deve ter um nome.");
        }
        setToolTipText(text);
        getAccessibleContext().setAccessibleName(text);
        setIcon(UIHelper.icon("fas-chevron-down", 12));
        popup.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UIHelper.BORDER),
                new EmptyBorder(6, 6, 6, 6)));
        addActionListener(e -> popup.show(this, 0, getHeight()));
    }

    public ActionMenuButton addAction(String label, Icon icon, Runnable action) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("A acção do menu deve ter um nome.");
        }
        if (popup.getComponentCount() >= MAX_ACTIONS) {
            throw new IllegalStateException("O menu de acções não pode ter mais de cinco opções.");
        }
        JMenuItem item = new JMenuItem(label, icon);
        item.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        item.setBorder(new EmptyBorder(8, 10, 8, 14));
        item.getAccessibleContext().setAccessibleName(label);
        item.addActionListener(e -> {
            if (action != null) action.run();
        });
        popup.add(item);
        return this;
    }

    int actionCount() {
        return popup.getComponentCount();
    }

    JMenuItem actionAt(int index) {
        return (JMenuItem) popup.getComponent(index);
    }
}


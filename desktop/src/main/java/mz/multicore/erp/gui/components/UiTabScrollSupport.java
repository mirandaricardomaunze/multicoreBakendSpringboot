package mz.multicore.erp.gui.components;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.UIResource;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;

/** Setas com tamanho de clique e roda do rato para as abas que não cabem na janela. */
final class UiTabScrollSupport {

    private static final int BUTTON_SIZE = 30;

    private UiTabScrollSupport() {}

    /** UIResource é obrigatório: sem ele, o JTabbedPane trataria o botão como uma aba nova. */
    static JButton button(int direction) {
        return new TabScrollButton(direction);
    }

    static void installMouseWheel(JTabbedPane tabs) {
        tabs.addMouseWheelListener(event -> {
            if (overTabStrip(tabs, event)) {
                scroll(tabs, event.getWheelRotation() > 0);
            } else if (tabs.getParent() != null) {
                tabs.getParent().dispatchEvent(SwingUtilities.convertMouseEvent(tabs, event, tabs.getParent()));
            }
        });
    }

    private static boolean overTabStrip(JTabbedPane tabs, MouseWheelEvent event) {
        if (tabs.getTabCount() == 0) return false;
        Rectangle first = tabs.getUI().getTabBounds(tabs, 0);
        return event.getY() <= first.y + first.height;
    }

    private static void scroll(JTabbedPane tabs, boolean forward) {
        Action action = tabs.getActionMap().get(forward ? "scrollTabsForwardAction" : "scrollTabsBackwardAction");
        if (action != null) action.actionPerformed(new ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, "scroll"));
    }

    private static final class TabScrollButton extends JButton implements UIResource {

        private final int direction;

        private TabScrollButton(int direction) {
            this.direction = direction;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(direction == SwingConstants.EAST ? "Ver abas seguintes" : "Ver abas anteriores");
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(BUTTON_SIZE, BUTTON_SIZE);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        /** Desenhado aqui porque o JTabbedPane associa uma acção sem ícone a este botão. */
        @Override
        protected void paintComponent(Graphics g) {
            String glyph = direction == SwingConstants.EAST ? "fas-chevron-right" : "fas-chevron-left";
            Icon icon = UIHelper.icon(glyph, 14, isEnabled() ? UIHelper.ACCENT : UIHelper.GRID);
            icon.paintIcon(this, g, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);
        }
    }
}

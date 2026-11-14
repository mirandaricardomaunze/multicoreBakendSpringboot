package mz.multicore.erp.gui.components;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * "Mais ▾" at the end of a tab strip that does not fit: lists the hidden tabs instead of making the user
 * scroll with arrows to discover them. The tab UI asks for two scroll buttons of equal width; together
 * they draw one "Mais ▾" control and both open the same list.
 */
final class UiTabOverflow {

    static final String LABEL = "Mais";
    private static final int HALF_WIDTH = 34;
    private static final int HEIGHT = 30;

    private UiTabOverflow() {}

    static JButton button(JTabbedPane tabs, int direction) {
        return new MoreButton(tabs, direction == SwingConstants.EAST);
    }

    /** Tabs not fully visible in the strip, in strip order. */
    static List<Integer> hiddenTabs(JTabbedPane tabs) {
        List<Integer> hidden = new ArrayList<>();
        Rectangle visible = null;
        for (Component child : tabs.getComponents()) {
            if (child instanceof JViewport viewport) visible = viewport.getBounds();
        }
        if (visible == null) return hidden;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Rectangle tab = tabs.getBoundsAt(i);
            if (tab == null || !visible.contains(tab)) hidden.add(i);
        }
        return hidden;
    }

    static JPopupMenu menu(JTabbedPane tabs) {
        JPopupMenu menu = new JPopupMenu();
        for (int index : hiddenTabs(tabs)) {
            String title = TabTitles.titleAt(tabs, index);
            Icon icon = tabs.getIconAt(index);
            JMenuItem item = new JMenuItem(title, icon == null ? null : UiTabIcons.tinted(icon, UIHelper.TEXT_MUTED));
            item.setFont(new Font(UIHelper.FONT, index == tabs.getSelectedIndex() ? Font.BOLD : Font.PLAIN, 13));
            item.setEnabled(tabs.isEnabledAt(index));
            item.addActionListener(event -> tabs.setSelectedIndex(index));
            menu.add(item);
        }
        return menu;
    }

    private static final class MoreButton extends JButton implements UIResource {

        private final JTabbedPane tabs;
        private final boolean chevronHalf;

        private MoreButton(JTabbedPane tabs, boolean chevronHalf) {
            this.tabs = tabs;
            this.chevronHalf = chevronHalf;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Ver os separadores que não cabem");
            getAccessibleContext().setAccessibleName("Mais separadores");
        }

        @Override public Dimension getPreferredSize() { return new Dimension(HALF_WIDTH, HEIGHT); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        /** The tab UI disables a scroll button at the end of the strip; this one always lists what is hidden. */
        @Override public void setEnabled(boolean enabled) { super.setEnabled(true); }

        /** Opens the list instead of notifying the tab UI, which would only scroll by one tab. */
        @Override
        protected void fireActionPerformed(ActionEvent event) {
            JPopupMenu menu = menu(tabs);
            if (menu.getComponentCount() == 0) return;
            int anchorX = chevronHalf ? -HALF_WIDTH : 0;
            menu.show(this, anchorX, getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            if (chevronHalf) {
                Icon chevron = UIHelper.icon("fas-chevron-down", 11, hover ? UIHelper.ACCENT : UIHelper.TEXT_MUTED);
                chevron.paintIcon(this, g2, 3, (getHeight() - chevron.getIconHeight()) / 2);
            } else {
                g2.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
                g2.setColor(hover ? UIHelper.ACCENT : UIHelper.TEXT_MUTED);
                FontMetrics metrics = g2.getFontMetrics();
                int x = getWidth() - metrics.stringWidth(LABEL) - 2;
                g2.drawString(LABEL, Math.max(0, x), (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            }
            g2.dispose();
        }
    }
}

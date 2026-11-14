package mz.multicore.erp.gui.components;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;

/**
 * Header of a table card. One row (title, filters, actions) when the filters fit at their natural width;
 * otherwise two rows, title and actions on top and the filters across the full width below, so filters are
 * never squeezed into "Tod…". Every part is vertically centred on its row.
 * The title hides itself when the enclosing tab already says it (see {@link TabTitles#repeatsTab}).
 */
final class TableCardTop {

    static final String HEADING = "tableCardTop.heading";
    static final int GAP = 12;
    static final int ROW_GAP = 8;

    private TableCardTop() {}

    static JPanel build(String title, JComponent filters, JComponent... actions) {
        JButton peek = takePeekToggle(filters, actions);
        if (peek != null) {
            actions = withoutComponent(actions, peek);
            filters = withPeekAtEnd(filters, peek);
        }
        JPanel top = new JPanel(new RowLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel heading = UIHelper.createSubheading(title);
        heading.putClientProperty(HEADING, title);
        top.add(heading, BorderLayout.WEST);
        if (filters != null) top.add(filters, BorderLayout.CENTER);
        JComponent[] arranged = TableHeaderActions.arrange(actions);
        if (arranged.length > 0) top.add(UIHelper.actionsBar(arranged), BorderLayout.EAST);

        top.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED)) != 0) {
                heading.setVisible(!TabTitles.repeatsTab(heading, title));
            }
        });
        return top;
    }

    /** The row-details toggle ("Espreitar…") always sits at the end of the filters, as a quiet icon button. */
    static boolean isPeekToggle(Component c) {
        if (!(c instanceof JButton button)) return false;
        String name = button.getAccessibleContext().getAccessibleName();
        String tip = button.getToolTipText();
        return (name != null && name.startsWith("Espreitar")) || (tip != null && tip.startsWith("Espreitar"));
    }

    private static JButton takePeekToggle(JComponent filters, JComponent[] actions) {
        if (actions != null) {
            for (JComponent action : actions) if (isPeekToggle(action)) return (JButton) action;
        }
        JButton found = filters == null ? null : findPeek(filters);
        if (found != null && found.getParent() != null) {
            Container parent = found.getParent();
            parent.remove(found);
            parent.revalidate();
        }
        return found;
    }

    private static JButton findPeek(Container root) {
        for (Component child : root.getComponents()) {
            if (isPeekToggle(child)) return (JButton) child;
            if (child instanceof Container nested) {
                JButton inner = findPeek(nested);
                if (inner != null) return inner;
            }
        }
        return null;
    }

    private static JComponent[] withoutComponent(JComponent[] actions, JComponent removed) {
        if (actions == null) return null;
        return java.util.Arrays.stream(actions).filter(a -> a != removed).toArray(JComponent[]::new);
    }

    private static JComponent withPeekAtEnd(JComponent filters, JButton peek) {
        if (peek instanceof ModernButton modern && !ButtonStyles.isOutline(modern)) ButtonStyles.outline(modern);
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        if (filters != null) row.add(filters, BorderLayout.CENTER);
        row.add(peek, BorderLayout.EAST);
        return row;
    }

    /** BorderLayout positions (WEST title, CENTER filters, EAST actions) laid out as one or two rows. */
    static final class RowLayout extends BorderLayout {

        private int lastRequested = -1;

        RowLayout() {
            super(GAP, 0);
        }

        static boolean fitsOneRow(int width, int title, int filters, int actions) {
            int parts = (title > 0 ? 1 : 0) + (filters > 0 ? 1 : 0) + (actions > 0 ? 1 : 0);
            return title + filters + actions + Math.max(0, parts - 1) * GAP <= width;
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            synchronized (target.getTreeLock()) {
                Insets in = target.getInsets();
                Component title = part(WEST), filters = part(CENTER), actions = part(EAST);
                int width = target.getWidth() > 0 ? target.getWidth() - in.left - in.right : Integer.MAX_VALUE;
                int oneRow = Math.max(height(title), Math.max(height(filters), height(actions)));
                int sumWidth = width(title) + width(filters) + width(actions) + 2 * GAP;
                int height = fitsOneRow(width, width(title), width(filters), width(actions)) || filters == null
                        ? oneRow
                        : Math.max(height(title), height(actions)) + ROW_GAP + height(filters);
                return new Dimension(Math.min(sumWidth, width) + in.left + in.right, height + in.top + in.bottom);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return new Dimension(0, preferredLayoutSize(target).height);
        }

        @Override
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                Insets in = target.getInsets();
                int x0 = in.left, y0 = in.top;
                int width = target.getWidth() - in.left - in.right;
                Component title = part(WEST), filters = part(CENTER), actions = part(EAST);
                int tw = width(title), aw = Math.min(width(actions), width);

                if (filters == null || fitsOneRow(width, tw, width(filters), width(actions))) {
                    int row = Math.max(height(title), Math.max(height(filters), height(actions)));
                    int x = x0;
                    if (title != null) { place(title, x, y0, tw, row); x += tw + GAP; }
                    int right = x0 + width - aw;
                    if (actions != null) place(actions, right, y0, aw, row);
                    if (filters != null) place(filters, x, y0, Math.max(0, (actions != null ? right - GAP : x0 + width) - x), row);
                    requestHeight(target, row + in.top + in.bottom);
                } else {
                    int first = Math.max(height(title), height(actions));
                    if (title != null) place(title, x0, y0, Math.min(tw, Math.max(0, width - aw - GAP)), first);
                    if (actions != null) place(actions, x0 + width - aw, y0, aw, first);
                    int second = y0 + (first > 0 ? first + ROW_GAP : 0);
                    place(filters, x0, second, width, height(filters));
                    requestHeight(target, second + height(filters) + in.bottom);
                }
            }
        }

        private Component part(Object constraint) {
            Component c = getLayoutComponent(constraint);
            return c != null && c.isVisible() ? c : null;
        }

        private static int width(Component c) { return c == null ? 0 : c.getPreferredSize().width; }
        private static int height(Component c) { return c == null ? 0 : c.getPreferredSize().height; }

        /** Vertically centred at its own preferred height, so filters and buttons line up. */
        private static void place(Component c, int x, int y, int width, int rowHeight) {
            int h = Math.min(c.getPreferredSize().height, rowHeight);
            c.setBounds(x, y + (rowHeight - h) / 2, width, h);
        }

        /** The number of rows depends on the width, known only now: ask the parent again if it changed. */
        private void requestHeight(Container target, int needed) {
            if (target.getHeight() != needed && needed != lastRequested && target instanceof JComponent component) {
                lastRequested = needed;
                SwingUtilities.invokeLater(component::revalidate);
            }
        }
    }
}

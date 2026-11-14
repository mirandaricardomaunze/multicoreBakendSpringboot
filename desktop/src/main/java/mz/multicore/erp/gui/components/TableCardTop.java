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
import java.util.ArrayList;
import java.util.List;

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
        if (arranged.length > 0) top.add(actionsPanel(arranged), BorderLayout.EAST);
        ModernButton main = TableHeaderActions.mainOf(arranged);
        TableHeaderShortcuts.install(top, main, arranged, filters);
        JComponent filtersArea = filters;

        top.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED)) != 0) {
                heading.setVisible(!TabTitles.repeatsTab(heading, title));
                linkToTable(top, main, filtersArea);
            }
        });
        return top;
    }

    /**
     * Tells the table of the same card which action and which filters belong to it, so its empty state
     * can offer "Nova Cotação" or "Limpar filtros". Only when the card holds exactly one table.
     */
    private static void linkToTable(JComponent top, ModernButton main, JComponent filters) {
        Container card = top.getParent();
        if (card == null) return;
        List<javax.swing.JTable> tables = new ArrayList<>();
        for (Component sibling : card.getComponents()) {
            if (sibling != top) collectTables(sibling, tables);
        }
        if (tables.size() != 1) return;
        javax.swing.JTable table = tables.get(0);
        if (main != null) table.putClientProperty(TableEmptyState.EMPTY_ACTION, main);
        if (filters != null) table.putClientProperty(TableEmptyState.EMPTY_FILTERS, filters);
    }

    private static void collectTables(Component root, List<javax.swing.JTable> out) {
        if (root instanceof javax.swing.JTable table) {
            out.add(table);
            return;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) collectTables(child, out);
        }
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
        // Same size as a field and pinned to the first line of controls, so it never hangs lower than them.
        java.awt.Dimension square = new java.awt.Dimension(UIHelper.FORM_CONTROL_HEIGHT, UIHelper.FORM_CONTROL_HEIGHT);
        peek.setPreferredSize(square);
        peek.setMinimumSize(square);
        peek.setMaximumSize(square);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(peek, BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        if (filters != null) row.add(filters, BorderLayout.CENTER);
        row.add(holder, BorderLayout.EAST);
        return row;
    }

    private static JPanel actionsPanel(JComponent[] actions) {
        JPanel bar = new JPanel(new ActionsLayout());
        bar.setOpaque(false);
        for (JComponent action : actions) bar.add(action);
        return bar;
    }

    /** Buttons right-aligned on one line; on a narrow card they wrap onto more right-aligned lines. */
    static final class ActionsLayout implements java.awt.LayoutManager {

        static final int HGAP = 8;
        static final int VGAP = 6;

        static int heightFor(Component actions, int width) {
            if (!(actions instanceof Container bar)) return 0;
            int lineWidth = 0, lineHeight = 0, total = 0;
            for (Component c : bar.getComponents()) {
                if (!c.isVisible()) continue;
                Dimension d = c.getPreferredSize();
                if (lineWidth > 0 && lineWidth + HGAP + d.width > width) {
                    total += lineHeight + VGAP;
                    lineWidth = 0;
                    lineHeight = 0;
                }
                lineWidth += (lineWidth > 0 ? HGAP : 0) + d.width;
                lineHeight = Math.max(lineHeight, d.height);
            }
            return total + lineHeight;
        }

        @Override
        public Dimension preferredLayoutSize(Container bar) {
            int width = 0, height = 0;
            for (Component c : bar.getComponents()) {
                if (!c.isVisible()) continue;
                Dimension d = c.getPreferredSize();
                width += (width > 0 ? HGAP : 0) + d.width;
                height = Math.max(height, d.height);
            }
            return new Dimension(width, height);
        }

        @Override
        public Dimension minimumLayoutSize(Container bar) {
            return new Dimension(0, preferredLayoutSize(bar).height);
        }

        @Override
        public void layoutContainer(Container bar) {
            List<List<Component>> lines = new ArrayList<>();
            List<Component> line = new ArrayList<>();
            int lineWidth = 0;
            for (Component c : bar.getComponents()) {
                if (!c.isVisible()) continue;
                int w = c.getPreferredSize().width;
                if (!line.isEmpty() && lineWidth + HGAP + w > bar.getWidth()) {
                    lines.add(line);
                    line = new ArrayList<>();
                    lineWidth = 0;
                }
                lineWidth += (line.isEmpty() ? 0 : HGAP) + w;
                line.add(c);
            }
            if (!line.isEmpty()) lines.add(line);
            int y = lines.size() == 1 ? Math.max(0, (bar.getHeight() - lineHeight(lines.get(0))) / 2) : 0;
            for (List<Component> row : lines) {
                int h = lineHeight(row);
                int x = bar.getWidth();
                for (int i = row.size() - 1; i >= 0; i--) {
                    Component c = row.get(i);
                    Dimension d = c.getPreferredSize();
                    x -= d.width;
                    c.setBounds(Math.max(0, x), y + (h - d.height) / 2, Math.min(d.width, bar.getWidth()), d.height);
                    x -= HGAP;
                }
                y += h + VGAP;
            }
        }

        private static int lineHeight(List<Component> row) {
            int h = 0;
            for (Component c : row) h = Math.max(h, c.getPreferredSize().height);
            return h;
        }

        @Override public void addLayoutComponent(String name, Component comp) { }
        @Override public void removeLayoutComponent(Component comp) { }
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

        private enum Mode { ONE_ROW, TITLE_AND_ACTIONS_THEN_FILTERS, STACKED }

        /**
         * ONE_ROW when everything fits at natural width; otherwise filters drop to their own row; and when
         * even the title and the buttons do not fit side by side (a narrow list/detail column), the buttons
         * get their own right-aligned row, wrapping onto more lines if needed.
         */
        private Mode mode(int width, Component title, Component filters, Component actions) {
            int tw = width(title), aw = width(actions);
            if (fitsOneRow(width, tw, width(filters), aw)) return Mode.ONE_ROW;
            if (fitsOneRow(width, tw, 0, aw)) return Mode.TITLE_AND_ACTIONS_THEN_FILTERS;
            return Mode.STACKED;
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            synchronized (target.getTreeLock()) {
                Insets in = target.getInsets();
                Component title = part(WEST), filters = part(CENTER), actions = part(EAST);
                int width = target.getWidth() > 0 ? target.getWidth() - in.left - in.right : Integer.MAX_VALUE;
                int sumWidth = width(title) + width(filters) + width(actions) + 2 * GAP;
                int height = switch (mode(width, title, filters, actions)) {
                    case ONE_ROW -> Math.max(band(title, filters, actions), height(filters));
                    case TITLE_AND_ACTIONS_THEN_FILTERS -> stack(Math.max(height(title), height(actions)), height(filters));
                    case STACKED -> stack(stack(height(title), ActionsLayout.heightFor(actions, width)), height(filters));
                };
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
                int tw = Math.min(width(title), width), aw = Math.min(width(actions), width);
                int bottom;
                switch (mode(width, title, filters, actions)) {
                    case ONE_ROW -> {
                        // Title and buttons line up with the first line of controls; anything the filters
                        // show below it (a hint, a second line) hangs underneath instead of pushing them down.
                        int band = band(title, filters, actions);
                        int x = x0;
                        if (title != null) { place(title, x, y0, tw, band); x += tw + GAP; }
                        int right = x0 + width - aw;
                        if (actions != null) place(actions, right, y0, aw, band);
                        if (filters != null) {
                            int fh = height(filters);
                            int fy = fh <= band ? y0 + (band - fh) / 2 : y0;
                            filters.setBounds(x, fy, Math.max(0, (actions != null ? right - GAP : x0 + width) - x), fh);
                        }
                        bottom = y0 + Math.max(band, height(filters));
                    }
                    case TITLE_AND_ACTIONS_THEN_FILTERS -> {
                        int first = Math.max(height(title), height(actions));
                        if (title != null) place(title, x0, y0, tw, first);
                        if (actions != null) place(actions, x0 + width - aw, y0, aw, first);
                        bottom = placeBelow(filters, x0, y0 + first, width);
                    }
                    default -> {
                        int y = y0;
                        if (title != null) { place(title, x0, y, tw, height(title)); y += height(title); }
                        if (actions != null) {
                            if (y > y0) y += ROW_GAP;
                            int ah = ActionsLayout.heightFor(actions, width);
                            actions.setBounds(x0, y, width, ah);
                            y += ah;
                        }
                        bottom = placeBelow(filters, x0, y, width);
                    }
                }
                requestHeight(target, bottom + in.bottom);
            }
        }

        private static int band(Component title, Component filters, Component actions) {
            return Math.max(height(title), Math.max(height(actions), Math.min(height(filters), UIHelper.FORM_CONTROL_HEIGHT)));
        }

        private static int stack(int above, int below) {
            if (above == 0) return below;
            return below == 0 ? above : above + ROW_GAP + below;
        }

        private static int placeBelow(Component c, int x, int y, int width) {
            if (c == null) return y;
            int top = y + ROW_GAP;
            c.setBounds(x, top, width, height(c));
            return top + height(c);
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

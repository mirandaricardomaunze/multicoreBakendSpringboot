package mz.multicore.erp.gui.sector;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Responsive building blocks for the sector screens, so they fit a laptop as well as a wide monitor:
 * <ul>
 *   <li>{@link #widthTracking(LayoutManager)}: a panel that takes the width of its scroll viewport, so a scroll
 *       pane only ever scrolls vertically and nothing is cut on the left or right;</li>
 *   <li>{@link #wrappingText(String, Font, Color)}: text that wraps at word boundaries and reports the right
 *       height for the width it gets (HTML labels do not, and get clipped);</li>
 *   <li>{@link #adaptiveColumns(JPanel, GridLayout, int)}: a grid whose number of columns follows the width.</li>
 * </ul>
 */
public final class ResponsiveSupport {

    private ResponsiveSupport() {
    }

    /**
     * Card header like {@code UIHelper.tableCardTop}, but the action buttons wrap to a second line on narrow windows
     * instead of covering the title or disappearing past the right edge. Filters keep their own shrinking layout.
     */
    public static JPanel cardTop(String title, JComponent filters, JComponent... actions) {
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, filters != null ? 6 : 4, 0));
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JLabel heading = UIHelper.createSubheading(title);
        heading.setVerticalAlignment(SwingConstants.TOP);
        header.add(heading, BorderLayout.WEST);
        if (actions != null && actions.length > 0) {
            JPanel bar = new JPanel(new WrapLayout(FlowLayout.RIGHT, 6, 0));
            bar.setOpaque(false);
            for (JComponent action : actions) if (action != null) bar.add(action);
            header.add(bar, BorderLayout.CENTER);
        }
        top.add(header, BorderLayout.NORTH);
        if (filters != null) top.add(filters, BorderLayout.CENTER);
        return top;
    }

    /** Panel that always takes the viewport width when placed in a {@link JScrollPane}. */
    public static JPanel widthTracking(LayoutManager layout) {
        return new WidthTrackingPanel(layout);
    }

    /** Vertical-only scroll pane around a width-tracking content. */
    public static JScrollPane verticalScroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        UIHelper.styleScrollPane(scroll);
        // The shared styling sets "as needed" on both bars; this content never scrolls sideways.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    /** Read-only text that wraps by words, transparent, with the given font and colour. */
    public static JTextArea wrappingText(String text, Font font, Color color) {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(new EmptyBorder(0, 0, 0, 0));
        area.setFont(font);
        area.setForeground(color);
        area.setHighlighter(null);
        // A small preferred width lets the layout shrink it; the real width comes from the parent.
        area.setColumns(8);
        return area;
    }

    /**
     * Keeps {@code layout}'s column count at {@code width / minCellWidth} (at least one) while {@code grid} is resized.
     * The grid should live inside a {@link #widthTracking(LayoutManager)} panel so it receives the viewport width.
     */
    public static void adaptiveColumns(JPanel grid, GridLayout layout, int minCellWidth) {
        grid.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int columns = columnsFor(grid.getWidth(), minCellWidth, layout.getHgap());
                if (columns != layout.getColumns()) {
                    layout.setColumns(columns);
                    grid.revalidate();
                    grid.repaint();
                }
            }
        });
    }

    /** Like {@link #adaptiveColumns(JPanel, GridLayout, int)} but never more columns than {@code maxColumns}. */
    public static void adaptiveColumns(JPanel grid, GridLayout layout, int minCellWidth, int maxColumns) {
        grid.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int columns = Math.min(Math.max(1, maxColumns), columnsFor(grid.getWidth(), minCellWidth, layout.getHgap()));
                if (columns != layout.getColumns()) {
                    layout.setColumns(columns);
                    grid.revalidate();
                    grid.repaint();
                }
            }
        });
    }

    /**
     * List and detail side by side on wide windows, one above the other below {@code minSideBySideWidth}, so neither
     * gets squeezed to an unreadable column on a laptop.
     */
    public static void adaptiveSplit(JSplitPane split, int minSideBySideWidth) {
        double sideWeight = split.getResizeWeight();
        split.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int wanted = split.getWidth() >= minSideBySideWidth ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
                if (wanted == split.getOrientation()) return;
                split.setOrientation(wanted);
                split.setResizeWeight(wanted == JSplitPane.HORIZONTAL_SPLIT ? sideWeight : 0.55);
                SwingUtilities.invokeLater(() -> split.setDividerLocation(split.getResizeWeight()));
            }
        });
    }

    /** Number of columns that fit {@code width} with cells of at least {@code minCellWidth}. */
    public static int columnsFor(int width, int minCellWidth, int gap) {
        if (width <= 0 || minCellWidth <= 0) return 1;
        return Math.max(1, (width + gap) / (minCellWidth + gap));
    }

    private static final class WidthTrackingPanel extends JPanel implements Scrollable {

        WidthTrackingPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }

        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(18, visible.height - 36);
        }

        @Override public boolean getScrollableTracksViewportWidth() { return true; }

        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }
    }
}

package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * Compact KPI tile: title above the value, note beside the value, small icon on the left.
 * Reuses the labels the card factory created, so screens keep updating them with setText.
 */
final class KpiCompactLayout {

    static final int HEIGHT = 56;
    private static final int ICON_SIZE = 16;
    private static final int TITLE_SIZE = 10;
    private static final int VALUE_SIZE = 17;
    private static final int NOTE_SIZE = 11;
    private static final String PARTS = "kpi.parts";
    private static final String COMPACT = "kpi.compact";
    private static final Locale PT = Locale.forLanguageTag("pt-MZ");

    /** The labels a KPI card is made of; kept on the card so it can be re-laid later. */
    record Parts(JLabel title, String iconCode, JLabel icon, JLabel value, TrendBadge trend, JLabel note) {}

    private KpiCompactLayout() {}

    static void remember(ModernPanel card, Parts parts) {
        card.putClientProperty(PARTS, parts);
    }

    static boolean isCompact(Component component) {
        return component instanceof JComponent c && Boolean.TRUE.equals(c.getClientProperty(COMPACT));
    }

    static void apply(ModernPanel card) {
        if (!(card.getClientProperty(PARTS) instanceof Parts parts) || isCompact(card)) return;
        card.removeAll();
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(6, 14, 6, 12));
        card.setPreferredSize(new Dimension(card.getPreferredSize().width, HEIGHT));
        card.setMinimumSize(new Dimension(KpiCard.STANDARD_CARD_MIN_WIDTH, HEIGHT));

        if (parts.icon() != null && parts.iconCode() != null) {
            Color accent = card.getClientProperty("kpi.accent") instanceof Color c ? c : parts.title().getForeground();
            parts.icon().setIcon(UIHelper.icon(parts.iconCode(), ICON_SIZE, accent));
            card.add(parts.icon(), BorderLayout.WEST);
        }

        JPanel text = new JPanel(new BorderLayout(0, 1));
        text.setOpaque(false);
        parts.title().setText(parts.title().getText() == null ? "" : parts.title().getText().toUpperCase(PT));
        resize(parts.title(), Font.BOLD, TITLE_SIZE);
        text.add(parts.title(), BorderLayout.NORTH);
        text.add(valueRow(parts), BorderLayout.CENTER);
        card.add(text, BorderLayout.CENTER);

        card.putClientProperty(COMPACT, Boolean.TRUE);
        card.revalidate();
        card.repaint();
    }

    private static JPanel valueRow(Parts parts) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new ValueRowLayout(parts.value(), parts.trend(), parts.note()));
        if (parts.value() != null) {
            parts.value().putClientProperty(KpiTextFit.FIT_AVAILABLE, (IntSupplier) () -> ValueRowLayout.valueRoom(row, parts.trend()));
            resize(parts.value(), Font.BOLD, VALUE_SIZE);
            row.add(parts.value());
        }
        if (parts.trend() != null) row.add(parts.trend());
        if (parts.note() != null) {
            JLabel note = parts.note();
            note.putClientProperty(KpiTextFit.FIT_AVAILABLE, (IntSupplier) note::getWidth);
            resize(note, Font.PLAIN, NOTE_SIZE);
            row.add(note);
        }
        return row;
    }

    /**
     * One line: value first, then the trend badge, then the note in whatever width is left.
     * The value has priority: its font shrinks first, the note is hidden next, and only then the value
     * ends in an ellipsis (with the full text in its tooltip). Nothing is ever drawn past the card edge.
     */
    static final class ValueRowLayout implements LayoutManager {
        private static final int TREND_GAP = 6;
        private static final int NOTE_GAP = 8;
        private static final int MIN_NOTE_WIDTH = 28;
        private final JLabel value;
        private final Component trend;
        private final JLabel note;

        ValueRowLayout(JLabel value, Component trend, JLabel note) {
            this.value = value;
            this.trend = trend;
            this.note = note;
        }

        static int valueRoom(Container row, Component trend) {
            Insets in = row.getInsets();
            int width = row.getWidth() - in.left - in.right;
            return trend == null ? width : width - trend.getPreferredSize().width - TREND_GAP;
        }

        @Override
        public void layoutContainer(Container row) {
            if (value != null) KpiTextFit.refit(value);
            Insets in = row.getInsets();
            int width = row.getWidth() - in.left - in.right;
            int height = row.getHeight() - in.top - in.bottom;
            int x = in.left;
            if (value != null) {
                int w = Math.max(0, Math.min(value.getPreferredSize().width, valueRoom(row, trend)));
                place(value, x, w, in.top, height);
                x += w;
            }
            if (trend != null) {
                x += TREND_GAP;
                int w = Math.max(0, Math.min(trend.getPreferredSize().width, in.left + width - x));
                place(trend, x, w, in.top, height);
                x += w;
            }
            if (note != null) {
                int room = in.left + width - x - NOTE_GAP;
                if (room >= MIN_NOTE_WIDTH) place(note, x + NOTE_GAP, room, in.top, height);
                else note.setBounds(0, 0, 0, 0);
                KpiTextFit.refit(note);
            }
        }

        private static void place(Component c, int x, int width, int top, int height) {
            int h = Math.min(c.getPreferredSize().height, height);
            c.setBounds(x, top + (height - h) / 2, width, h);
        }

        @Override
        public Dimension preferredLayoutSize(Container row) {
            int width = 0;
            int height = 0;
            for (Component c : row.getComponents()) {
                Dimension d = c.getPreferredSize();
                width += d.width;
                height = Math.max(height, d.height);
            }
            if (trend != null) width += TREND_GAP;
            if (note != null) width += NOTE_GAP;
            Insets in = row.getInsets();
            return new Dimension(width + in.left + in.right, height + in.top + in.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container row) {
            return new Dimension(0, preferredLayoutSize(row).height);
        }

        @Override public void addLayoutComponent(String name, Component comp) {}
        @Override public void removeLayoutComponent(Component comp) {}
    }

    private static void resize(JLabel label, int style, int size) {
        label.setFont(new Font(UIHelper.FONT, style, size));
        KpiTextFit.refit(label, size);
    }
}

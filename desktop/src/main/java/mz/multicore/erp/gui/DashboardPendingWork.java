package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * "Operações e Pendências" as an actionable list: one row per kind of pending work with its count,
 * opening the screen that resolves it. Replaces a bar chart that drew three unrelated counts as bars.
 */
final class DashboardPendingWork extends JPanel {

    static final String TITLE = "Operações e Pendências";

    record Item(String label, String hint, String iconCode, Color tone, long count, String targetModule) { }

    private final Consumer<String> navigationHandler;
    private final JPanel rows = new JPanel();

    DashboardPendingWork(Consumer<String> navigationHandler) {
        super(new BorderLayout(0, Spacing.SM));
        this.navigationHandler = navigationHandler;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM));

        JLabel title = new JLabel(TITLE);
        title.setFont(Typography.TITLE);
        title.setForeground(UIHelper.TEXT_LIGHT);
        add(title, BorderLayout.NORTH);

        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        add(rows, BorderLayout.CENTER);
    }

    void setItems(List<Item> items) {
        rows.removeAll();
        if (items.stream().allMatch(i -> i.count() <= 0)) {
            rows.add(allClear());
        } else {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) rows.add(Box.createVerticalStrut(Spacing.XS));
                rows.add(row(items.get(i)));
            }
        }
        rows.add(Box.createVerticalGlue());
        revalidate();
        repaint();
    }

    private JComponent allClear() {
        JPanel panel = new JPanel(new BorderLayout(0, Spacing.XS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(Spacing.XL, 0, Spacing.XL, 0));
        JLabel icon = new JLabel(UIHelper.icon("fas-check-circle", 30, UIHelper.APPROVED_GREEN), SwingConstants.CENTER);
        JLabel message = new JLabel(DashboardOverviewTab.NO_PENDING_MESSAGE, SwingConstants.CENTER);
        message.setFont(Typography.EMPHASIS_BOLD);
        message.setForeground(UIHelper.TEXT_LIGHT);
        JLabel hint = new JLabel("Sem aprovações, tickets ou stock baixo por tratar.", SwingConstants.CENTER);
        hint.setFont(Typography.BODY);
        hint.setForeground(UIHelper.TEXT_MUTED);
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(message, BorderLayout.NORTH);
        text.add(hint, BorderLayout.SOUTH);
        panel.add(icon, BorderLayout.NORTH);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private JComponent row(Item item) {
        boolean pending = item.count() > 0;
        Color tone = pending ? item.tone() : UIHelper.TEXT_MUTED;

        HoverRow row = new HoverRow();
        row.setLayout(new BorderLayout(Spacing.MD, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        row.add(new IconDot(UIHelper.icon(item.iconCode(), 14, tone), UIHelper.withAlpha(tone, pending ? 28 : 16)), BorderLayout.WEST);

        JLabel label = new JLabel(item.label());
        label.setFont(Typography.BODY_BOLD);
        label.setForeground(UIHelper.TEXT_LIGHT);
        JLabel hint = new JLabel(pending ? item.hint() : "Nada por tratar");
        hint.setFont(Typography.CAPTION);
        hint.setForeground(UIHelper.TEXT_MUTED);
        JPanel text = new JPanel(new BorderLayout(0, 1));
        text.setOpaque(false);
        text.add(label, BorderLayout.NORTH);
        text.add(hint, BorderLayout.SOUTH);
        row.add(text, BorderLayout.CENTER);

        JLabel count = new JLabel(String.valueOf(item.count()));
        count.setFont(Typography.TITLE);
        count.setForeground(pending ? tone : UIHelper.TEXT_MUTED);
        JLabel chevron = new JLabel(UIHelper.icon("fas-chevron-right", 11, UIHelper.TEXT_MUTED));
        JPanel trailing = new JPanel(new BorderLayout(Spacing.SM, 0));
        trailing.setOpaque(false);
        trailing.add(count, BorderLayout.CENTER);
        trailing.add(chevron, BorderLayout.EAST);
        row.add(trailing, BorderLayout.EAST);

        if (navigationHandler != null && item.targetModule() != null) {
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.setToolTipText("Abrir " + item.label().toLowerCase());
            row.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { navigationHandler.accept(item.targetModule()); }
            });
        }
        return row;
    }

    /** Row with a soft rounded background on hover, the same affordance as clickable KPI cards. */
    private static final class HoverRow extends JPanel {
        private boolean hover;

        HoverRow() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) {
                    if (!contains(e.getPoint())) { hover = false; repaint(); }
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? UIHelper.ROW_ALT : UIHelper.withAlpha(UIHelper.ROW_ALT, 120));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), UIHelper.RADIUS_MD, UIHelper.RADIUS_MD);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Icon centred on a soft tinted circle. */
    private static final class IconDot extends JLabel {
        private final Color fill;

        IconDot(javax.swing.Icon icon, Color fill) {
            super(icon, SwingConstants.CENTER);
            this.fill = fill;
            setPreferredSize(new Dimension(34, 34));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            int d = Math.min(getWidth(), getHeight());
            g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

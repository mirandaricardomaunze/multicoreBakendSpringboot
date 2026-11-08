package mz.multicore.erp.gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * Horizontal progress bar for multi-step forms: numbered circles joined by a line, the current
 * step in the accent colour, finished steps in green with a check mark, later steps muted.
 * A step the caller allows ({@link #setNavigable}) can be clicked to jump to it.
 */
public class StepIndicator extends JComponent {

    private static final int DIAMETER = 26;
    private static final int BAR_HEIGHT = 58;

    private final List<String> labels;
    private int current;
    private IntPredicate navigable = index -> false;
    private IntConsumer onSelect = index -> {};

    public StepIndicator(List<String> labels) {
        this.labels = List.copyOf(labels);
        setOpaque(false);
        setPreferredSize(new Dimension(120 * labels.size(), BAR_HEIGHT));
        setMinimumSize(new Dimension(80 * labels.size(), BAR_HEIGHT));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = indexAt(e.getX());
                if (index >= 0 && index != current && navigable.test(index)) onSelect.accept(index);
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = indexAt(e.getX());
                boolean clickable = index >= 0 && index != current && navigable.test(index);
                setCursor(clickable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
        getAccessibleContext().setAccessibleName("Passos do formulário");
    }

    /** A bare JComponent has no accessible context of its own; without this it is null. */
    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PROGRESS_BAR;
                }
            };
        }
        return accessibleContext;
    }

    public void setCurrent(int index) {
        this.current = Math.max(0, Math.min(labels.size() - 1, index));
        getAccessibleContext().setAccessibleDescription(
                "Passo " + (current + 1) + " de " + labels.size() + ": " + labels.get(current));
        repaint();
    }

    public int current() {
        return current;
    }

    /** Which steps may be opened by clicking, and what to do when one is clicked. */
    public void setNavigable(IntPredicate navigable, IntConsumer onSelect) {
        this.navigable = navigable == null ? index -> false : navigable;
        this.onSelect = onSelect == null ? index -> {} : onSelect;
    }

    private int indexAt(int x) {
        if (labels.isEmpty() || getWidth() <= 0) return -1;
        int slot = getWidth() / labels.size();
        int index = x / Math.max(1, slot);
        return index >= 0 && index < labels.size() ? index : -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int count = labels.size();
            int slot = getWidth() / Math.max(1, count);
            int cy = 6 + DIAMETER / 2;

            for (int i = 0; i < count - 1; i++) {
                int x1 = slot * i + slot / 2 + DIAMETER / 2 + 6;
                int x2 = slot * (i + 1) + slot / 2 - DIAMETER / 2 - 6;
                g2.setColor(i < current ? UIHelper.APPROVED_GREEN : UIHelper.GRID);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, cy, x2, cy);
            }

            Font numberFont = new Font(UIHelper.FONT, Font.BOLD, 12);
            Font labelFont = new Font(UIHelper.FONT, Font.PLAIN, 12);
            Font currentLabelFont = new Font(UIHelper.FONT, Font.BOLD, 12);
            for (int i = 0; i < count; i++) {
                int cx = slot * i + slot / 2;
                Ellipse2D circle = new Ellipse2D.Double(cx - DIAMETER / 2.0, cy - DIAMETER / 2.0, DIAMETER, DIAMETER);
                boolean done = i < current;
                boolean active = i == current;
                if (done || active) {
                    g2.setColor(done ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_BLUE);
                    g2.fill(circle);
                } else {
                    g2.setColor(UIHelper.BG_CARD);
                    g2.fill(circle);
                    g2.setColor(UIHelper.BORDER);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(circle);
                }

                if (done) {
                    Icon check = UIHelper.icon("fas-check", 12, Color.WHITE);
                    check.paintIcon(this, g2, cx - check.getIconWidth() / 2, cy - check.getIconHeight() / 2);
                } else {
                    String number = String.valueOf(i + 1);
                    g2.setFont(numberFont);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(active ? Color.WHITE : UIHelper.TEXT_MUTED);
                    g2.drawString(number, cx - fm.stringWidth(number) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
                }

                g2.setFont(active ? currentLabelFont : labelFont);
                g2.setColor(active ? UIHelper.TEXT_LIGHT : UIHelper.TEXT_MUTED);
                String label = fit(labels.get(i), g2.getFontMetrics(), slot - 8);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + DIAMETER / 2 + 18);
            }
        } finally {
            g2.dispose();
        }
    }

    private static String fit(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) return text;
        String ellipsis = "…";
        for (int end = text.length() - 1; end > 0; end--) {
            String candidate = text.substring(0, end).trim() + ellipsis;
            if (fm.stringWidth(candidate) <= width) return candidate;
        }
        return ellipsis;
    }
}

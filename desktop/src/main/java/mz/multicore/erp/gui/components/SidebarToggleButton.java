package mz.multicore.erp.gui.components;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Botão de alternância estilizado com chevron vetorial e acessível para a barra lateral retrátil.
 */
public class SidebarToggleButton extends JButton {

    private boolean collapsed = false;
    private boolean hover = false;

    public SidebarToggleButton() {
        setPreferredSize(new Dimension(30, 30));
        setMaximumSize(new Dimension(30, 30));
        setMinimumSize(new Dimension(30, 30));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Recolher menu lateral (Ctrl+B)");
        getAccessibleContext().setAccessibleName("Recolher menu lateral");

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
        });
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        setToolTipText(collapsed ? "Expandir menu lateral (Ctrl+B)" : "Recolher menu lateral (Ctrl+B)");
        getAccessibleContext().setAccessibleName(collapsed ? "Expandir menu lateral" : "Recolher menu lateral");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            Color bg = hover ? (UIHelper.isLight() ? new Color(0, 0, 0, 22) : new Color(255, 255, 255, 28))
                             : (UIHelper.isLight() ? new Color(0, 0, 0, 8) : new Color(255, 255, 255, 12));
            g2.setColor(bg);
            g2.fillRoundRect(1, 1, w - 2, h - 2, 6, 6);
            g2.setColor(new Color(UIHelper.BORDER.getRed(), UIHelper.BORDER.getGreen(), UIHelper.BORDER.getBlue(), hover ? 140 : 60));
            g2.drawRoundRect(1, 1, w - 2, h - 2, 6, 6);

            Color glyphColor = UIHelper.isLight() ? new Color(71, 85, 105) : new Color(226, 232, 240);
            g2.setColor(glyphColor);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = w / 2;
            int cy = h / 2;
            if (collapsed) {
                g2.drawLine(cx - 2, cy - 5, cx + 3, cy);
                g2.drawLine(cx + 3, cy, cx - 2, cy + 5);
            } else {
                g2.drawLine(cx + 2, cy - 5, cx - 3, cy);
                g2.drawLine(cx - 3, cy, cx + 2, cy + 5);
            }
        } finally {
            g2.dispose();
        }
    }
}

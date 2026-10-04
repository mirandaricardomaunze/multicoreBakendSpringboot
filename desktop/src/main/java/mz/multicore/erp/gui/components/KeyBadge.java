package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Componente visual que renderiza um badge estilizado em formato de tecla física de teclado (ex: [F9], [Enter], [Esc]).
 * Melhora a ergonomia e curva de aprendizagem no POS e ecrãs operacionais.
 */
public class KeyBadge extends JComponent {

    private final String keyText;
    private Color badgeBackground = new Color(0, 0, 0, 80);
    private Color badgeBorder = new Color(255, 255, 255, 60);
    private Color textColor = Color.WHITE;

    public KeyBadge(String keyText) {
        this.keyText = keyText == null ? "" : keyText.trim();
        setOpaque(false);
        setBorder(new EmptyBorder(2, 6, 2, 6));
        setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
    }

    public static KeyBadge of(String key) {
        return new KeyBadge(key);
    }

    public String getKeyText() {
        return keyText;
    }

    public void setColors(Color background, Color border, Color text) {
        this.badgeBackground = background;
        this.badgeBorder = border;
        this.textColor = text;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(keyText) + 14;
        int h = fm.getHeight() + 4;
        return new Dimension(Math.max(w, 24), Math.max(h, 18));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 6;

            // Fundo da tecla
            g2.setColor(badgeBackground);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

            // Borda da tecla
            g2.setColor(badgeBorder);
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            // Texto da tecla
            g2.setFont(getFont());
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            int textX = (w - fm.stringWidth(keyText)) / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(keyText, textX, textY);
        } finally {
            g2.dispose();
        }
    }
}

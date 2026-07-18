package com.phcpro.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * ScrollBar fina estilo PHC / ERP profissional.
 * Thumb arredondado de 6 px com a cor de acento a 60% de opacidade;
 * track transparente; setas removidas (dimensão zero).
 * Aplicar via {@link UIHelper#styleScrollPane(JScrollPane)}.
 */
public class SlimScrollBarUI extends BasicScrollBarUI {

    /** Largura/espessura do thumb e da barra (px). */
    private static final int THICKNESS = 6;

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    /** Botão de dimensão zero — remove visualmente as setas da barra. */
    private JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        b.setVisible(false);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Track transparente — o ScrollPane já tem o fundo do painel.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Thumb: cor de acento com 60% de opacidade — visível mas não intrusivo
        Color thumbColor = new Color(
                UIHelper.ACCENT.getRed(),
                UIHelper.ACCENT.getGreen(),
                UIHelper.ACCENT.getBlue(),
                153); // ~60% alpha
        g2.setColor(thumbColor);
        // Padding de 1 px para o thumb não colar às extremidades do track
        int x = thumbBounds.x + 1;
        int y = thumbBounds.y + 1;
        int w = thumbBounds.width - 2;
        int h = thumbBounds.height - 2;
        int r = Math.min(w, h); // raio = min(largura, altura) → totalmente arredondado
        g2.fillRoundRect(x, y, w, h, r, r);
        g2.dispose();
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(THICKNESS, THICKNESS * 4);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(THICKNESS + 2, THICKNESS * 4)
                : new Dimension(THICKNESS * 4, THICKNESS + 2);
    }
}

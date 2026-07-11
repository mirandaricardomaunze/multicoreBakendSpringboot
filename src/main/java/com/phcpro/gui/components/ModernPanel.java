package com.phcpro.gui.components;

import javax.swing.*;
import java.awt.*;

public class ModernPanel extends JPanel {

    private int cornerRadius = UIHelper.RADIUS_LG;
    private boolean isGradient = false;
    private Color gradientStart;
    private Color gradientEnd;

    public ModernPanel() {
        setOpaque(false);
        setBackground(UIHelper.BG_CARD); // fundo de cartão do tema activo
    }

    public ModernPanel(int radius) {
        this();
        this.cornerRadius = radius;
    }

    /** Fundo sólido — para painéis de conteúdo (não KPI). */
    public ModernPanel(int radius, Color backgroundColor) {
        this(radius);
        setBackground(backgroundColor);
    }

    /** Gradiente — para KPI cards do dashboard. */
    public ModernPanel(int radius, Color start, Color end) {
        this(radius);
        this.isGradient = true;
        this.gradientStart = start;
        this.gradientEnd = end;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background
        if (isGradient && gradientStart != null && gradientEnd != null) {
            GradientPaint gp = new GradientPaint(0, 0, gradientStart, 0, height, gradientEnd);
            g2.setPaint(gp);
        } else {
            g2.setColor(getBackground());
        }

        g2.fillRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        // Border — adapta ao tema:
        // • sobre gradiente (KPI card): branco translúcido subtil
        // • painel normal: cor BORDER do tema activo (funciona em claro E escuro)
        Color borderColor = isGradient
                ? new Color(255, 255, 255, 20)
                : UIHelper.BORDER;
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        g2.dispose();
    }
}

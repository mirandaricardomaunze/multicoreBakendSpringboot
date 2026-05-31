package com.phcpro.gui.components;

import javax.swing.*;
import java.awt.*;

public class ModernPanel extends JPanel {

    private int cornerRadius = 16;
    private Color shadowColor = new Color(0, 0, 0, 40);
    private Color borderColor = new Color(255, 255, 255, 15);
    private boolean isGradient = false;
    private Color gradientStart;
    private Color gradientEnd;

    public ModernPanel() {
        setOpaque(false);
        setBackground(new Color(31, 41, 55)); // Default Tailwind Gray-800 (#1F2937)
    }

    public ModernPanel(int radius) {
        this();
        this.cornerRadius = radius;
    }

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

        // Border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        g2.dispose();
    }
}

package com.phcpro.gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {

    private Color normalColor = new Color(59, 130, 246); // Tailwind Blue-500 (#3B82F6)
    private Color hoverColor = new Color(37, 99, 235);   // Tailwind Blue-600 (#2563EB)
    private Color clickColor = new Color(29, 78, 216);   // Tailwind Blue-700 (#1D4ED8)
    private Color textColor = Color.WHITE;
    private int cornerRadius = 20;

    private boolean isGradient = false;
    private Color gradientStart = new Color(139, 92, 246); // Violet-500
    private Color gradientEnd = new Color(59, 130, 246);   // Blue-500

    public ModernButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(textColor);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(normalColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(clickColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (getBounds().contains(e.getPoint())) {
                    setBackground(hoverColor);
                } else {
                    setBackground(normalColor);
                }
            }
        });
        setBackground(normalColor);
    }

    public ModernButton(String text, Color baseColor, Color hover) {
        this(text);
        this.normalColor = baseColor;
        this.hoverColor = hover;
        this.clickColor = baseColor.darker();
        setBackground(normalColor);
    }

    public void setGradient(Color start, Color end) {
        this.isGradient = true;
        this.gradientStart = start;
        this.gradientEnd = end;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        Color start = gradientStart != null ? gradientStart : new Color(139, 92, 246);
        Color end = gradientEnd != null ? gradientEnd : new Color(59, 130, 246);
        Color bg = getBackground() != null ? getBackground() : new Color(59, 130, 246);

        // Button background
        if (isGradient && getModel() != null && getModel().isRollover()) {
            GradientPaint gp = new GradientPaint(0, 0, start.brighter(), 0, height, end.brighter());
            g2.setPaint(gp);
        } else if (isGradient) {
            GradientPaint gp = new GradientPaint(0, 0, start, 0, height, end);
            g2.setPaint(gp);
        } else {
            g2.setColor(bg);
        }

        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        g2.dispose();

        super.paintComponent(g);
    }
}

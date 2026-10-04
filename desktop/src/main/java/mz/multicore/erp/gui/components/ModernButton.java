package mz.multicore.erp.gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {

    private Color normalColor = new Color(59, 130, 246); // Tailwind Blue-500 (#3B82F6)
    private Color hoverColor = new Color(37, 99, 235);   // Tailwind Blue-600 (#2563EB)
    private Color clickColor = new Color(29, 78, 216);   // Tailwind Blue-700 (#1D4ED8)
    private Color textColor = Color.WHITE;
    private int cornerRadius = UIHelper.RADIUS_MD;

    private boolean isGradient = false;
    private Color gradientStart = new Color(139, 92, 246); // Violet-500
    private Color gradientEnd = new Color(59, 130, 246);   // Blue-500

    private float hoverProgress = 0.0f;
    private Timer hoverTimer;
    private static final int HOVER_ANIM_DURATION = 120;
    private static final int HOVER_ANIM_STEPS = 6;
    private static final int HOVER_STEP_MS = HOVER_ANIM_DURATION / HOVER_ANIM_STEPS;

    private String shortcutText;

    public ModernButton setShortcut(String shortcutText) {
        this.shortcutText = shortcutText != null && !shortcutText.isBlank() ? shortcutText.trim() : null;
        revalidate();
        repaint();
        return this;
    }

    public String getShortcut() {
        return shortcutText;
    }

    public static String stripEllipsis(String text) {
        if (text == null) return "";
        String s = text.trim();
        while (s.endsWith("...") || s.endsWith("…")) {
            if (s.endsWith("...")) {
                s = s.substring(0, s.length() - 3).trim();
            } else if (s.endsWith("…")) {
                s = s.substring(0, s.length() - 1).trim();
            }
        }
        return s;
    }

    public ModernButton(String text) {
        super(stripEllipsis(text));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        setBackground(normalColor);
        setForeground(textColor);
        setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
                AbstractButton b = (AbstractButton) c;
                FontMetrics fm = g.getFontMetrics();
                int mnemonicIndex = b.getDisplayedMnemonicIndex();
                if (b.isEnabled()) {
                    g.setColor(b.getForeground());
                } else {
                    Color bg = b.getBackground();
                    Color readable = UIHelper.readableTextOn(bg != null ? bg : Color.WHITE);
                    if (Color.WHITE.equals(readable)) {
                        g.setColor(new Color(255, 255, 255, 140));
                    } else {
                        g.setColor(new Color(107, 114, 128, 180));
                    }
                }
                String drawText = text;
                if (drawText != null && (drawText.endsWith("...") || drawText.endsWith("…"))) {
                    String full = b.getText();
                    drawText = (full != null && !full.isBlank()) ? stripEllipsis(full) : stripEllipsis(drawText);
                } else if (drawText != null) {
                    drawText = stripEllipsis(drawText);
                }
                javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(g, drawText, mnemonicIndex,
                        textRect.x + getTextShiftOffset(),
                        textRect.y + fm.getAscent() + getTextShiftOffset());
            }

            @Override
            protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
                AbstractButton b = (AbstractButton) c;
                Icon icon = b.getIcon();
                if (icon != null) {
                    icon.paintIcon(c, g, iconRect.x + getTextShiftOffset(),
                            iconRect.y + getTextShiftOffset());
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) animateHover(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) animateHover(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(clickColor);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    if (getBounds().contains(e.getPoint())) {
                        setBackground(UIHelper.blendColors(normalColor, hoverColor, hoverProgress));
                    } else {
                        animateHover(false);
                    }
                    repaint();
                }
            }
        });
        setBackground(normalColor);
    }

    public float getHoverProgress() {
        return hoverProgress;
    }

    public void setHoverProgress(float progress) {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        this.hoverProgress = Math.max(0.0f, Math.min(1.0f, progress));
        updateHoverColor();
    }

    public void animateHover(boolean entering) {
        if (!isDisplayable() || GraphicsEnvironment.isHeadless()) {
            setHoverProgress(entering ? 1.0f : 0.0f);
            return;
        }
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        float step = 1.0f / HOVER_ANIM_STEPS;
        hoverTimer = new Timer(HOVER_STEP_MS, e -> {
            if (entering) {
                hoverProgress = Math.min(1.0f, hoverProgress + step);
            } else {
                hoverProgress = Math.max(0.0f, hoverProgress - step);
            }
            updateHoverColor();
            if ((entering && hoverProgress >= 1.0f) || (!entering && hoverProgress <= 0.0f)) {
                ((Timer) e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }

    private void updateHoverColor() {
        Color blended = UIHelper.blendColors(normalColor, hoverColor, hoverProgress);
        super.setBackground(blended);
        repaint();
    }

    public ModernButton(String text, Color baseColor, Color hover) {
        this(text);
        this.normalColor = baseColor;
        this.hoverColor = hover;
        this.clickColor = baseColor.darker();
        setBackground(normalColor);
        this.textColor = UIHelper.readableTextOn(baseColor);
        setForeground(this.textColor);
    }

    /** Redefine as cores do botão em runtime (ex.: alternar estado activo/inactivo num segmented control). */
    public void setColors(Color base, Color hover) {
        if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
        this.hoverProgress = 0.0f;
        this.normalColor = base;
        this.hoverColor = hover;
        this.clickColor = base.darker();
        this.isGradient = false;
        setBackground(base);
        this.textColor = UIHelper.readableTextOn(base);
        setForeground(this.textColor);
        if (getIcon() instanceof org.kordamp.ikonli.swing.FontIcon fi) {
            fi.setIconColor(this.textColor);
        }
        repaint();
    }

    @Override
    public void setIcon(Icon icon) {
        if (icon instanceof org.kordamp.ikonli.swing.FontIcon fi) {
            fi.setIconColor(this.textColor != null ? this.textColor : Color.WHITE);
        }
        super.setIcon(icon);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (bg != null && this.textColor != null && UIHelper.contrastRatio(bg, this.textColor) < 3.0) {
            Color safeFg = UIHelper.readableTextOn(bg);
            super.setForeground(safeFg);
            this.textColor = safeFg;
            if (getIcon() instanceof org.kordamp.ikonli.swing.FontIcon fi) {
                fi.setIconColor(safeFg);
            }
        }
    }

    @Override
    public void setForeground(Color fg) {
        Color safeFg = fg;
        Color bg = getBackground();
        if (bg != null && fg != null && UIHelper.contrastRatio(bg, fg) < 3.0) {
            safeFg = UIHelper.readableTextOn(bg);
        }
        super.setForeground(safeFg);
        this.textColor = safeFg;
        if (getIcon() instanceof org.kordamp.ikonli.swing.FontIcon fi) {
            fi.setIconColor(safeFg != null ? safeFg : Color.WHITE);
        }
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
    public void setText(String text) {
        super.setText(stripEllipsis(text));
    }

    public int calculateShortcutBadgeWidth() {
        if (shortcutText == null || shortcutText.isEmpty()) return 0;
        Font bf = new Font(Font.MONOSPACED, Font.BOLD, 10);
        FontMetrics fm = getFontMetrics(bf);
        return (fm != null ? fm.stringWidth(shortcutText) : 24) + 12;
    }

    public int getBaseRightInset() {
        javax.swing.border.Border b = getBorder();
        return b != null ? b.getBorderInsets(this).right : 14;
    }

    @Override
    public Insets getInsets() {
        Insets insets = super.getInsets();
        if (shortcutText != null && !shortcutText.isEmpty()) {
            int extra = calculateShortcutBadgeWidth() + 8;
            return new Insets(insets.top, insets.left, insets.bottom, insets.right + extra);
        }
        return insets;
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets ins = super.getInsets(insets);
        if (shortcutText != null && !shortcutText.isEmpty()) {
            int extra = calculateShortcutBadgeWidth() + 8;
            ins.right += extra;
        }
        return ins;
    }

    /**
     * Garante que os botões têm pelo menos a altura dos campos de formulário
     * ({@link UIHelper#FORM_CONTROL_HEIGHT}), para botões e inputs alinharem na mesma linha.
     * A largura garante margem para nunca truncar texto com reticências (...).
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Font font = getFont() != null ? getFont() : new Font(UIHelper.FONT, Font.BOLD, 13);
        FontMetrics fm = getFontMetrics(font);
        int textW = (getText() != null && fm != null) ? fm.stringWidth(getText()) : 0;
        int iconW = (getIcon() != null) ? (getIcon().getIconWidth() + Math.max(getIconTextGap(), 8)) : 0;
        Insets insets = getInsets();
        int insetsW = insets != null ? (insets.left + insets.right) : 28;
        int neededW = textW + iconW + insetsW + 12;

        if (isPreferredSizeSet() && d != null) {
            return new Dimension(Math.max(d.width, neededW), Math.max(d.height, UIHelper.FORM_CONTROL_HEIGHT));
        }
        int minW = Math.max(d != null ? d.width + 12 : neededW, Math.max(neededW, 76));
        return new Dimension(minW, Math.max(d != null ? d.height : 0, UIHelper.FORM_CONTROL_HEIGHT));
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension pref = getPreferredSize();
        Dimension d = super.getMinimumSize();
        if (d == null) return pref;
        return new Dimension(Math.max(d.width, pref.width), Math.max(d.height, pref.height));
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Feedback táctil: ligeiro deslocamento de 1px ao premir
        if (getModel() != null && getModel().isPressed()) {
            g2.translate(0, 1);
        }

        Color start = gradientStart != null ? gradientStart : new Color(139, 92, 246);
        Color end = gradientEnd != null ? gradientEnd : new Color(59, 130, 246);
        Color bg = getBackground() != null ? getBackground() : new Color(59, 130, 246);

        // Button background
        if (isGradient && hoverProgress > 0.0f) {
            Color s = UIHelper.blendColors(start, start.brighter(), hoverProgress);
            Color e = UIHelper.blendColors(end, end.brighter(), hoverProgress);
            GradientPaint gp = new GradientPaint(0, 0, s, 0, height, e);
            g2.setPaint(gp);
        } else if (isGradient) {
            GradientPaint gp = new GradientPaint(0, 0, start, 0, height, end);
            g2.setPaint(gp);
        } else {
            g2.setColor(bg);
        }

        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        g2.dispose();

        // Salvaguarda visual inegociável: nunca renderizar texto branco sobre fundo claro
        Color currentFg = getForeground();
        if (bg != null && (currentFg == null || UIHelper.contrastRatio(bg, currentFg) < 3.0)) {
            Color safeFg = UIHelper.readableTextOn(bg);
            super.setForeground(safeFg);
            this.textColor = safeFg;
            if (getIcon() instanceof org.kordamp.ikonli.swing.FontIcon fi) {
                fi.setIconColor(safeFg);
            }
        } else if (!isEnabled()) {
            if (getIcon() instanceof org.kordamp.ikonli.swing.FontIcon fi) {
                Color baseIconColor = this.textColor != null ? this.textColor : Color.WHITE;
                fi.setIconColor(new Color(baseIconColor.getRed(), baseIconColor.getGreen(), baseIconColor.getBlue(), 120));
            }
        } else {
            if (getIcon() instanceof org.kordamp.ikonli.swing.FontIcon fi) {
                fi.setIconColor(this.textColor != null ? this.textColor : Color.WHITE);
            }
        }

        super.paintComponent(g);

        // Badge estilizado de tecla de atalho (ex: [F9], [Enter]) à direita
        if (shortcutText != null && !shortcutText.isEmpty()) {
            Graphics2D g3 = (Graphics2D) g.create();
            try {
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Font badgeFont = new Font(Font.MONOSPACED, Font.BOLD, 10);
                g3.setFont(badgeFont);
                FontMetrics fm = g3.getFontMetrics();
                int bw = calculateShortcutBadgeWidth();
                int bh = fm.getHeight() + 2;
                int rightPad = getBaseRightInset();
                int bx = width - rightPad - bw;
                int by = (height - bh) / 2;

                g3.setColor(new Color(0, 0, 0, 75));
                g3.fillRoundRect(bx, by, bw, bh, 6, 6);
                g3.setColor(new Color(255, 255, 255, 70));
                g3.drawRoundRect(bx, by, bw, bh, 6, 6);

                Color fg = getForeground();
                g3.setColor(fg != null ? fg : Color.WHITE);
                int tx = bx + (bw - fm.stringWidth(shortcutText)) / 2;
                int ty = by + (bh - fm.getHeight()) / 2 + fm.getAscent();
                g3.drawString(shortcutText, tx, ty);
            } finally {
                g3.dispose();
            }
        }
    }
}

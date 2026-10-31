package mz.multicore.erp.gui.components;

import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {

    private Color normalColor = new Color(59, 130, 246);
    private Color hoverColor = new Color(37, 99, 235);
    private Color clickColor = new Color(29, 78, 216);
    private Color textColor = Color.WHITE;
    private int cornerRadius = UIHelper.RADIUS_MD;

    private boolean isGradient = false;
    private Color gradientStart = new Color(139, 92, 246);
    private Color gradientEnd = new Color(59, 130, 246);

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

    public String getShortcut() { return shortcutText; }

    public static String stripEllipsis(String text) { return ModernButtonPainter.stripEllipsis(text); }

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
        setUI(ModernButtonPainter.createButtonUI());

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) animateHover(true); }
            @Override public void mouseExited(MouseEvent e) { if (isEnabled()) animateHover(false); }
            @Override public void mousePressed(MouseEvent e) {
                if (isEnabled()) { setBackground(clickColor); repaint(); }
            }
            @Override public void mouseReleased(MouseEvent e) {
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

    public ModernButton(String text, Color baseColor, Color hover) {
        this(text);
        this.normalColor = baseColor;
        this.hoverColor = hover;
        this.clickColor = baseColor.darker();
        setBackground(normalColor);
        this.textColor = UIHelper.readableTextOn(baseColor);
        setForeground(this.textColor);
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        setCursor(new Cursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    public float getHoverProgress() { return hoverProgress; }

    public void setHoverProgress(float progress) {
        if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
        this.hoverProgress = Math.max(0.0f, Math.min(1.0f, progress));
        updateHoverColor();
    }

    public void animateHover(boolean entering) {
        if (!isDisplayable() || GraphicsEnvironment.isHeadless()) {
            setHoverProgress(entering ? 1.0f : 0.0f);
            return;
        }
        if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
        float step = 1.0f / HOVER_ANIM_STEPS;
        hoverTimer = new Timer(HOVER_STEP_MS, e -> {
            hoverProgress = entering ? Math.min(1.0f, hoverProgress + step) : Math.max(0.0f, hoverProgress - step);
            updateHoverColor();
            if ((entering && hoverProgress >= 1.0f) || (!entering && hoverProgress <= 0.0f)) {
                ((Timer) e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }

    private void updateHoverColor() {
        super.setBackground(UIHelper.blendColors(normalColor, hoverColor, hoverProgress));
        repaint();
    }

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
        if (getIcon() instanceof FontIcon fi) fi.setIconColor(this.textColor);
        repaint();
    }

    @Override
    public void setIcon(Icon icon) {
        if (icon instanceof FontIcon fi) fi.setIconColor(this.textColor != null ? this.textColor : Color.WHITE);
        super.setIcon(icon);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (bg != null && this.textColor != null && UIHelper.contrastRatio(bg, this.textColor) < 3.0) {
            applySafeForeground(UIHelper.readableTextOn(bg));
        }
    }

    @Override
    public void setForeground(Color fg) {
        Color bg = getBackground();
        Color safeFg = (bg != null && fg != null && UIHelper.contrastRatio(bg, fg) < 3.0) ? UIHelper.readableTextOn(bg) : fg;
        applySafeForeground(safeFg);
    }

    void applySafeForeground(Color safeFg) {
        super.setForeground(safeFg);
        this.textColor = safeFg;
        if (getIcon() instanceof FontIcon fi) fi.setIconColor(safeFg != null ? safeFg : Color.WHITE);
    }

    public void setGradient(Color start, Color end) {
        this.isGradient = true;
        this.gradientStart = start;
        this.gradientEnd = end;
        repaint();
    }

    public void setCornerRadius(int radius) { this.cornerRadius = radius; repaint(); }

    @Override public void setText(String text) { super.setText(stripEllipsis(text)); }

    public int calculateShortcutBadgeWidth() { return ModernButtonPainter.calculateShortcutBadgeWidth(this, shortcutText); }

    public int getBaseRightInset() {
        Border b = getBorder();
        return b != null ? b.getBorderInsets(this).right : 14;
    }

    @Override
    public Insets getInsets() {
        Insets insets = super.getInsets();
        return (shortcutText != null && !shortcutText.isEmpty())
                ? new Insets(insets.top, insets.left, insets.bottom, insets.right + calculateShortcutBadgeWidth() + 8)
                : insets;
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets ins = super.getInsets(insets);
        if (shortcutText != null && !shortcutText.isEmpty()) ins.right += calculateShortcutBadgeWidth() + 8;
        return ins;
    }

    @Override
    public Dimension getPreferredSize() { return ModernButtonPainter.computePreferredSize(this, super.getPreferredSize()); }

    @Override
    public Dimension getMinimumSize() {
        Dimension pref = getPreferredSize();
        Dimension d = super.getMinimumSize();
        return d == null ? pref : new Dimension(Math.max(d.width, pref.width), Math.max(d.height, pref.height));
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        ModernButtonPainter.paintButtonSurface(this, g, cornerRadius, isGradient, gradientStart, gradientEnd, hoverProgress);
        ModernButtonPainter.syncIconAndForegroundContrast(this, textColor);
        super.paintComponent(g);
        ModernButtonPainter.paintShortcutBadge(this, g, shortcutText);
    }
}

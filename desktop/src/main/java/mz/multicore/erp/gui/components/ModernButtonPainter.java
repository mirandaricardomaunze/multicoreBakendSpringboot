package mz.multicore.erp.gui.components;

import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;

final class ModernButtonPainter {

    private ModernButtonPainter() {}

    static String stripEllipsis(String text) {
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

    static ButtonUI createButtonUI() {
        return new BasicButtonUI() {
            @Override
            protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
                AbstractButton b = (AbstractButton) c;
                FontMetrics fm = g.getFontMetrics();
                int mnemonicIndex = b.getDisplayedMnemonicIndex();
                if (b.isEnabled()) {
                    g.setColor(b.getForeground());
                } else {
                    g.setColor(UIHelper.isLight() ? new Color(148, 163, 184) : new Color(100, 116, 139));
                }
                String drawText = text;
                if (drawText != null && (drawText.endsWith("...") || drawText.endsWith("…"))) {
                    String full = b.getText();
                    drawText = (full != null && !full.isBlank()) ? stripEllipsis(full) : stripEllipsis(drawText);
                } else if (drawText != null) {
                    drawText = stripEllipsis(drawText);
                }
                BasicGraphicsUtils.drawStringUnderlineCharAt(g, drawText, mnemonicIndex,
                        textRect.x + getTextShiftOffset(),
                        textRect.y + fm.getAscent() + getTextShiftOffset());
            }

            @Override
            protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
                AbstractButton b = (AbstractButton) c;
                Icon icon = b.getIcon();
                if (icon != null) {
                    icon.paintIcon(c, g, iconRect.x + getTextShiftOffset(), iconRect.y + getTextShiftOffset());
                }
            }
        };
    }

    static int calculateShortcutBadgeWidth(ModernButton btn, String shortcutText) {
        if (shortcutText == null || shortcutText.isEmpty()) return 0;
        Font bf = new Font(Font.MONOSPACED, Font.BOLD, 10);
        FontMetrics fm = btn.getFontMetrics(bf);
        return (fm != null ? fm.stringWidth(shortcutText) : 24) + 12;
    }

    static Dimension computePreferredSize(ModernButton btn, Dimension superPref) {
        Font font = btn.getFont() != null ? btn.getFont() : new Font(UIHelper.FONT, Font.BOLD, 13);
        FontMetrics fm = btn.getFontMetrics(font);
        int textW = (btn.getText() != null && fm != null) ? fm.stringWidth(btn.getText()) : 0;
        int iconW = (btn.getIcon() != null) ? (btn.getIcon().getIconWidth() + Math.max(btn.getIconTextGap(), 8)) : 0;
        Insets insets = btn.getInsets();
        int insetsW = insets != null ? (insets.left + insets.right) : 28;
        int strictMin = textW + iconW + insetsW;
        int neededW = strictMin + 12;

        if (btn.isPreferredSizeSet() && superPref != null) {
            return new Dimension(Math.max(superPref.width, strictMin), Math.max(superPref.height, UIHelper.FORM_CONTROL_HEIGHT));
        }
        int minW = Math.max(superPref != null ? superPref.width + 12 : neededW, Math.max(neededW, 76));
        return new Dimension(minW, Math.max(superPref != null ? superPref.height : 0, UIHelper.FORM_CONTROL_HEIGHT));
    }

    static void paintButtonSurface(ModernButton btn, Graphics g, int cornerRadius,
                                   boolean isGradient, Color gradientStart, Color gradientEnd, float hoverProgress) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = btn.getWidth();
        int height = btn.getHeight();
        if (btn.getModel() != null && btn.getModel().isPressed()) {
            g2.translate(0, 1);
        }

        Color start = gradientStart != null ? gradientStart : new Color(139, 92, 246);
        Color end = gradientEnd != null ? gradientEnd : new Color(59, 130, 246);
        Color bg = btn.getBackground() != null ? btn.getBackground() : new Color(59, 130, 246);

        if (!btn.isEnabled()) {
            Color disabledBg = UIHelper.isLight() ? new Color(226, 232, 240) : new Color(51, 65, 85);
            g2.setColor(disabledBg);
            g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
            g2.setColor(UIHelper.BORDER);
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        } else if (isGradient && hoverProgress > 0.0f) {
            Color s = UIHelper.blendColors(start, start.brighter(), hoverProgress);
            Color e = UIHelper.blendColors(end, end.brighter(), hoverProgress);
            g2.setPaint(new GradientPaint(0, 0, s, 0, height, e));
            g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        } else if (isGradient) {
            g2.setPaint(new GradientPaint(0, 0, start, 0, height, end));
            g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        } else {
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        }
        g2.dispose();
    }

    static void syncIconAndForegroundContrast(ModernButton btn, Color textColor) {
        Color bg = btn.getBackground() != null ? btn.getBackground() : new Color(59, 130, 246);
        Color currentFg = btn.getForeground();
        if (!btn.isEnabled()) {
            if (btn.getIcon() instanceof FontIcon fi) {
                fi.setIconColor(UIHelper.isLight() ? new Color(148, 163, 184) : new Color(100, 116, 139));
            }
        } else if (currentFg == null || UIHelper.contrastRatio(bg, currentFg) < 3.0) {
            Color safeFg = UIHelper.readableTextOn(bg);
            btn.applySafeForeground(safeFg);
        } else if (btn.getIcon() instanceof FontIcon fi) {
            fi.setIconColor(textColor != null ? textColor : Color.WHITE);
        }
    }

    static void paintShortcutBadge(ModernButton btn, Graphics g, String shortcutText) {
        if (shortcutText == null || shortcutText.isEmpty()) return;
        Graphics2D g3 = (Graphics2D) g.create();
        try {
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font badgeFont = new Font(Font.MONOSPACED, Font.BOLD, 10);
            g3.setFont(badgeFont);
            FontMetrics fm = g3.getFontMetrics();
            int bw = btn.calculateShortcutBadgeWidth();
            int bh = fm.getHeight() + 2;
            int bx = btn.getWidth() - btn.getBaseRightInset() - bw;
            int by = (btn.getHeight() - bh) / 2;

            g3.setColor(new Color(0, 0, 0, 75));
            g3.fillRoundRect(bx, by, bw, bh, 6, 6);
            g3.setColor(new Color(255, 255, 255, 70));
            g3.drawRoundRect(bx, by, bw, bh, 6, 6);

            Color fg = btn.getForeground();
            g3.setColor(fg != null ? fg : Color.WHITE);
            int tx = bx + (bw - fm.stringWidth(shortcutText)) / 2;
            int ty = by + (bh - fm.getHeight()) / 2 + fm.getAscent();
            g3.drawString(shortcutText, tx, ty);
        } finally {
            g3.dispose();
        }
    }
}

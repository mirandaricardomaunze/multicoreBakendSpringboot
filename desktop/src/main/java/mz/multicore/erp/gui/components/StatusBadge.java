package mz.multicore.erp.gui.components;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Componente visual em formato de pílula (Pill Badge) para exibição elegante de estados,
 * níveis e categorias em tabelas, cabeçalhos e cartões.
 */
public class StatusBadge extends JLabel {

    private final Color badgeColor;
    private final Color badgeBg;
    private final Color badgeBorder;

    public StatusBadge(String text, Color color) {
        this(text, color, null);
    }

    public StatusBadge(String text, Color color, String iconCode) {
        super(text != null ? text.toUpperCase() : "");
        this.badgeColor = color != null ? color : UIHelper.TEXT_MUTED;

        // Fundo semitransparente na cor de base para efeito "frosted glass / pill"
        boolean isDarkTheme = !UIHelper.isLight();
        int alpha = isDarkTheme ? 45 : 30;
        int borderAlpha = isDarkTheme ? 90 : 65;
        this.badgeBg = new Color(this.badgeColor.getRed(), this.badgeColor.getGreen(), this.badgeColor.getBlue(), alpha);
        this.badgeBorder = new Color(this.badgeColor.getRed(), this.badgeColor.getGreen(), this.badgeColor.getBlue(), borderAlpha);

        setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(new EmptyBorder(3, 10, 3, 10));

        // Cor do texto com alto contraste garantido
        Color fg = isDarkTheme ? this.badgeColor.brighter() : this.badgeColor.darker();
        if (UIHelper.contrastRatio(this.badgeBg, fg) < 3.5) {
            fg = UIHelper.readableTextOn(this.badgeBg);
        }
        setForeground(fg);

        if (iconCode != null && !iconCode.isBlank()) {
            Icon icon = UIHelper.icon(iconCode, 10, fg);
            setIcon(icon);
            setIconTextGap(5);
        }
        setOpaque(false);
    }

    public static StatusBadge success(String text) {
        return new StatusBadge(text, UIHelper.APPROVED_GREEN, "fas-check");
    }

    public static StatusBadge warning(String text) {
        return new StatusBadge(text, UIHelper.PENDING_YELLOW, "fas-exclamation-triangle");
    }

    public static StatusBadge danger(String text) {
        return new StatusBadge(text, UIHelper.REJECTED_RED, "fas-times-circle");
    }

    public static StatusBadge info(String text) {
        return new StatusBadge(text, UIHelper.ACCENT_BLUE, "fas-info-circle");
    }

    public static StatusBadge neutral(String text) {
        return new StatusBadge(text, UIHelper.TEXT_MUTED, null);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 8, 54), Math.max(d.height, 22));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = h; // Formato de cápsula perfeita

        // Fundo semitransparente
        g2.setColor(badgeBg);
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

        // Borda sutil
        g2.setColor(badgeBorder);
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}

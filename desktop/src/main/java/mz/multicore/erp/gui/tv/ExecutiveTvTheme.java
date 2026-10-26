package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Utilitário de tema e estilos visuais corporativos para o painel de TV Executiva.
 */
public final class ExecutiveTvTheme {

    private ExecutiveTvTheme() {}

    public static Color getBgColor(boolean lightMode) {
        return lightMode ? new Color(241, 245, 249) : new Color(15, 23, 42);
    }

    public static Color getCardBgColor(boolean lightMode) {
        return lightMode ? Color.WHITE : new Color(30, 41, 59);
    }

    public static Color getCardBorderColor(boolean lightMode) {
        return lightMode ? new Color(226, 232, 240) : new Color(51, 65, 85);
    }

    public static Color getTextLightColor(boolean lightMode) {
        return lightMode ? new Color(15, 23, 42) : new Color(248, 250, 252);
    }

    public static Color getTextMutedColor(boolean lightMode) {
        return lightMode ? new Color(100, 116, 139) : new Color(148, 163, 184);
    }

    public static Color getHeaderBgColor(boolean lightMode) {
        return lightMode ? new Color(255, 255, 255, 245) : new Color(15, 23, 42, 245);
    }

    public static JPanel buildTvKpi(String title, String value, String subtitle, Color accentColor, boolean lightMode) {
        Color kpiBg = lightMode ? new Color(248, 250, 252) : new Color(15, 23, 42, 180);
        Color kpiBorder = lightMode ? new Color(226, 232, 240) : new Color(51, 65, 85, 120);

        ModernPanel panel = new ModernPanel(10, kpiBg, kpiBg);
        panel.setLayout(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(kpiBorder, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(getTextMutedColor(lightMode));
        panel.add(titleLbl, BorderLayout.NORTH);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        valLbl.setForeground(accentColor);
        panel.add(valLbl, BorderLayout.CENTER);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        subLbl.setForeground(getTextMutedColor(lightMode));
        panel.add(subLbl, BorderLayout.SOUTH);

        return panel;
    }

    public static JLabel buildStatusBadge(double pct) {
        JLabel badge = new JLabel();
        badge.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        badge.setOpaque(true);

        Color fg;
        Color bg;
        String text;
        if (pct >= 100.0) {
            text = "  META SUPERADA  ";
            fg = new Color(16, 185, 129);
            bg = new Color(16, 185, 129, 30);
        } else if (pct >= 80.0) {
            text = "  NO RITMO  ";
            fg = new Color(59, 130, 246);
            bg = new Color(59, 130, 246, 30);
        } else if (pct >= 50.0) {
            text = "  ATENÇÃO  ";
            fg = new Color(245, 158, 11);
            bg = new Color(245, 158, 11, 30);
        } else {
            text = "  ALERTA  ";
            fg = new Color(239, 68, 68);
            bg = new Color(239, 68, 68, 30);
        }

        badge.setText(text);
        badge.setForeground(fg);
        badge.setBackground(bg);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 90), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        return badge;
    }
}

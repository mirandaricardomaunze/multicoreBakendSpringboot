package mz.multicore.erp.gui.tv;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;

/**
 * Tokens visuais do painel de TV Executiva: a única fonte de cores do pacote.
 * Tema escuro "Slate" (fundo azul-noite, cartões um tom acima) e claro "Executive Light".
 */
public final class ExecutiveTvTheme {

    private ExecutiveTvTheme() {}

    public static Color getBgColor(boolean lightMode) {
        return lightMode ? new Color(241, 245, 249) : new Color(9, 14, 26);
    }

    public static Color getHeaderBgColor(boolean lightMode) {
        return lightMode ? Color.WHITE : new Color(13, 19, 34);
    }

    public static Color getCardBgColor(boolean lightMode) {
        return lightMode ? Color.WHITE : new Color(17, 25, 42);
    }

    /** Superfície dentro de um cartão (linhas de lista, controlos). */
    public static Color getSurfaceAltColor(boolean lightMode) {
        return lightMode ? new Color(248, 250, 252) : new Color(24, 34, 55);
    }

    public static Color getCardBorderColor(boolean lightMode) {
        return lightMode ? new Color(226, 232, 240) : new Color(33, 45, 69);
    }

    public static Color getTrackColor(boolean lightMode) {
        return lightMode ? new Color(226, 232, 240) : new Color(30, 42, 64);
    }

    public static Color getTextLightColor(boolean lightMode) {
        return lightMode ? new Color(15, 23, 42) : new Color(241, 245, 249);
    }

    public static Color getTextMutedColor(boolean lightMode) {
        return lightMode ? new Color(100, 116, 139) : new Color(148, 163, 184);
    }

    public static Color getTextFaintColor(boolean lightMode) {
        return lightMode ? new Color(148, 163, 184) : new Color(94, 110, 135);
    }

    /** Paleta categórica das unidades de negócio (gráfico de participação). */
    public static Color seriesColor(int index) {
        Color[] palette = {
                new Color(59, 130, 246), new Color(16, 185, 129), new Color(245, 158, 11),
                new Color(139, 92, 246), new Color(236, 72, 153), new Color(20, 184, 166), new Color(249, 115, 22)
        };
        return palette[Math.floorMod(index, palette.length)];
    }

    /** Mistura opaca de {@code accent} sobre {@code base} (0 = base, 1 = accent). */
    public static Color tint(Color accent, Color base, double amount) {
        double a = Math.max(0, Math.min(1, amount));
        return new Color(
                (int) Math.round(base.getRed() + (accent.getRed() - base.getRed()) * a),
                (int) Math.round(base.getGreen() + (accent.getGreen() - base.getGreen()) * a),
                (int) Math.round(base.getBlue() + (accent.getBlue() - base.getBlue()) * a));
    }

    /** Cor em notação CSS, para as propriedades de estilo do FlatLaf. */
    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static JPanel buildTvKpi(String title, String value, String subtitle, Color accentColor, boolean lightMode) {
        return ExecutiveTvComponents.kpiTile(null, title, value, subtitle, accentColor, lightMode);
    }

    public static JLabel buildStatusBadge(double pct) {
        return buildStatusBadge(pct, mz.multicore.erp.gui.components.UIHelper.isLight());
    }

    public static JLabel buildStatusBadge(double pct, boolean lightMode) {
        return buildStatusBadge(GoalStatus.of(true, pct), lightMode);
    }

    public static JLabel buildStatusBadge(GoalStatus status, boolean lightMode) {
        return ExecutiveTvComponents.pill(status.label(), status.color(lightMode), lightMode);
    }
}

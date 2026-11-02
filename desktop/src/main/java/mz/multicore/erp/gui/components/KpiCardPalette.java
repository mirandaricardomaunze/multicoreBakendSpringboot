package mz.multicore.erp.gui.components;

import java.awt.Color;

import static mz.multicore.erp.gui.components.KpiCard.KpiPalette;

/** Gestor e resolvedor espectral de paletas de cores coordenadas para cartões de KPI. */
public final class KpiCardPalette {

    private KpiCardPalette() {}

    /** Fundo do tema e texto normal: a cor da métrica fica só na faixa e no ícone. */
    private static KpiPalette card(Color accent) {
        return new KpiPalette(UIHelper.BG_CARD, UIHelper.BORDER, UIHelper.TEXT_MUTED,
                UIHelper.TEXT_LIGHT, UIHelper.TEXT_MUTED, accent);
    }

    static boolean isAlert(KpiPalette palette) {
        boolean light = UIHelper.isLight();
        return palette.accent.equals(redPalette(light).accent) || palette.accent.equals(amberPalette(light).accent)
                || palette.accent.equals(orangePalette(light).accent);
    }

    public static KpiPalette bluePalette(boolean isLight) {
        return card(isLight ? new Color(37, 99, 235) : new Color(96, 165, 250));
    }

    public static KpiPalette emeraldPalette(boolean isLight) {
        return card(isLight ? new Color(5, 150, 105) : new Color(52, 211, 153));
    }

    public static KpiPalette purplePalette(boolean isLight) {
        return card(isLight ? new Color(124, 58, 237) : new Color(192, 132, 252));
    }

    public static KpiPalette cyanPalette(boolean isLight) {
        return card(isLight ? new Color(8, 145, 178) : new Color(34, 211, 238));
    }

    public static KpiPalette amberPalette(boolean isLight) {
        return card(isLight ? new Color(217, 119, 6) : new Color(251, 191, 36));
    }

    public static KpiPalette orangePalette(boolean isLight) {
        return card(isLight ? new Color(234, 88, 12) : new Color(251, 146, 60));
    }

    public static KpiPalette redPalette(boolean isLight) {
        return card(isLight ? new Color(220, 38, 38) : new Color(248, 113, 113));
    }

    public static KpiPalette slatePalette(boolean isLight) {
        return card(isLight ? new Color(100, 116, 139) : new Color(148, 163, 184));
    }

    public static KpiPalette resolvePalette(Color accent) {
        return resolvePalette(accent, null);
    }

    public static KpiPalette resolvePalette(Color accent, Color fallbackColor) {
        if (accent == null) accent = fallbackColor != null ? fallbackColor : UIHelper.ACCENT_BLUE;
        boolean isLight = UIHelper.isLight();

        if (accent.equals(UIHelper.KPI_INFO_SOFT) || accent.equals(UIHelper.KPI_INFO_DARK)
                || accent.equals(UIHelper.ACCENT_BLUE) || accent.equals(UIHelper.ACCENT_SKY)
                || accent.equals(UIHelper.ACCENT_BLUE_HOVER)) {
            return bluePalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_PURPLE_SOFT) || accent.equals(UIHelper.KPI_PURPLE_DARK)
                || accent.equals(UIHelper.KPI_PURPLE_END) || accent.equals(UIHelper.ACCENT)
                || accent.equals(UIHelper.ACCENT_HOVER)) {
            return purplePalette(isLight);
        }
        if (accent.equals(UIHelper.APPROVED_GREEN) || accent.equals(UIHelper.APPROVED_GREEN_HOVER)) {
            return emeraldPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_SUCCESS_SOFT) || accent.equals(UIHelper.KPI_INFO_END)
                || accent.equals(UIHelper.ACCENT_CYAN)) {
            if (fallbackColor != null && (fallbackColor.equals(UIHelper.APPROVED_GREEN) || fallbackColor.equals(UIHelper.APPROVED_GREEN_HOVER))) {
                return emeraldPalette(isLight);
            }
            return cyanPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_WARNING_SOFT) || accent.equals(UIHelper.KPI_WARNING_DARK)
                || accent.equals(UIHelper.KPI_WARNING_END) || accent.equals(UIHelper.PENDING_YELLOW)) {
            return amberPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_ORANGE_SOFT) || accent.equals(UIHelper.KPI_ORANGE_DARK)
                || accent.equals(UIHelper.KPI_ORANGE_END) || accent.equals(UIHelper.ACCENT_ORANGE)) {
            return orangePalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_DANGER_SOFT) || accent.equals(UIHelper.KPI_DANGER_DARK)
                || accent.equals(UIHelper.KPI_DANGER_END) || accent.equals(UIHelper.REJECTED_RED)
                || accent.equals(UIHelper.REJECTED_RED_HOVER)) {
            return redPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_NEUTRAL_SOFT) || accent.equals(UIHelper.KPI_NEUTRAL_DARK)
                || accent.equals(UIHelper.KPI_NEUTRAL_END) || accent.equals(UIHelper.TEXT_LIGHT)
                || accent.equals(UIHelper.TEXT_MUTED) || accent.equals(UIHelper.BUTTON_NEUTRAL)) {
            return slatePalette(isLight);
        }

        float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
        float hue = hsb[0] * 360f;
        float sat = hsb[1];

        if (sat < 0.18f) return slatePalette(isLight);
        if (hue >= 70 && hue < 160) return emeraldPalette(isLight);
        if (hue >= 160 && hue < 205) return cyanPalette(isLight);
        if (hue >= 205 && hue < 255) return bluePalette(isLight);
        if (hue >= 255 && hue < 325) return purplePalette(isLight);
        if (hue >= 325 || hue < 12) return redPalette(isLight);
        if (hue >= 12 && hue < 42) return orangePalette(isLight);
        return amberPalette(isLight);
    }

    public static Color[] resolveKpiGradient(Color accent) {
        KpiPalette pal = resolvePalette(accent);
        return new Color[]{pal.bg, pal.bg};
    }

    public static Color resolveKpiSoftColor(Color accent) {
        return resolvePalette(accent).title;
    }
}

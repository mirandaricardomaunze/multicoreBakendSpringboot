package mz.multicore.erp.gui.components;

import java.awt.Color;

import static mz.multicore.erp.gui.components.KpiCard.KpiPalette;

/** Gestor e resolvedor espectral de paletas de cores coordenadas para cartões de KPI. */
public final class KpiCardPalette {

    private KpiCardPalette() {}

    public static KpiPalette bluePalette(boolean isLight) {
        Color bg = isLight ? new Color(28, 68, 165) : new Color(22, 54, 138);
        return new KpiPalette(bg, new Color(96, 165, 250, 180), new Color(219, 234, 254), Color.WHITE, new Color(191, 219, 254));
    }

    public static KpiPalette emeraldPalette(boolean isLight) {
        Color bg = isLight ? new Color(5, 112, 82) : new Color(6, 92, 68);
        return new KpiPalette(bg, new Color(52, 211, 153, 180), new Color(209, 250, 229), Color.WHITE, new Color(167, 243, 208));
    }

    public static KpiPalette purplePalette(boolean isLight) {
        Color bg = isLight ? new Color(102, 38, 185) : new Color(82, 30, 155);
        return new KpiPalette(bg, new Color(192, 132, 252, 180), new Color(243, 232, 255), Color.WHITE, new Color(233, 213, 255));
    }

    public static KpiPalette cyanPalette(boolean isLight) {
        Color bg = isLight ? new Color(12, 122, 136) : new Color(11, 102, 115);
        return new KpiPalette(bg, new Color(34, 211, 238, 180), new Color(207, 250, 254), Color.WHITE, new Color(165, 243, 252));
    }

    public static KpiPalette amberPalette(boolean isLight) {
        Color bg = isLight ? new Color(162, 82, 10) : new Color(138, 68, 8);
        return new KpiPalette(bg, new Color(251, 191, 36, 180), new Color(254, 243, 199), Color.WHITE, new Color(253, 230, 138));
    }

    public static KpiPalette orangePalette(boolean isLight) {
        Color bg = isLight ? new Color(176, 62, 15) : new Color(152, 52, 12);
        return new KpiPalette(bg, new Color(251, 146, 60, 180), new Color(255, 237, 213), Color.WHITE, new Color(254, 215, 170));
    }

    public static KpiPalette redPalette(boolean isLight) {
        Color bg = isLight ? new Color(172, 26, 36) : new Color(148, 20, 28);
        return new KpiPalette(bg, new Color(248, 113, 113, 180), new Color(254, 226, 226), Color.WHITE, new Color(254, 202, 202));
    }

    public static KpiPalette slatePalette(boolean isLight) {
        Color bg = isLight ? new Color(51, 65, 85) : new Color(36, 48, 66);
        return new KpiPalette(bg, new Color(148, 163, 184, 180), new Color(241, 245, 249), Color.WHITE, new Color(203, 213, 225));
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

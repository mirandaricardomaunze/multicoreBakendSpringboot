package mz.multicore.erp.gui.components;

import java.awt.Color;

/**
 * Semantic colours of the application chrome, resolved for the active theme at call time
 * (docs/CORES_TEMA_SPEC.md).
 *
 * <p>Before, screens wrote {@code UIHelper.isLight() ? new Color(..) : new Color(..)}: two branches, so
 * the High Contrast theme fell into the dark branch and got low-contrast slate borders. Each token here
 * has its light, dark and high-contrast value side by side. Theme-wide surfaces and text stay in
 * {@link Theme} / {@link UIHelper} ({@code BG_CARD}, {@code BORDER}, {@code TEXT_LIGHT}…).
 */
public final class ThemeColors {

    private ThemeColors() {
    }

    /** Sidebar and top bar surface. */
    public static Color chromeBg() {
        return pick(rgb(255, 255, 255), rgb(24, 32, 47), rgb(0, 0, 0));
    }

    /** Line between the chrome and the content. */
    public static Color chromeBorder() {
        return pick(rgb(226, 232, 240), rgb(45, 55, 72), rgb(255, 255, 255));
    }

    /** Quiet separators and borders: lists, chart grid lines, popup frames. */
    public static Color hairline() {
        return pick(rgb(226, 232, 240), rgb(51, 65, 85), rgb(160, 160, 160));
    }

    /** Stronger frame for search boxes, key caps and focused popups. */
    public static Color outline() {
        return pick(rgb(203, 213, 225), rgb(71, 85, 105), rgb(255, 255, 255));
    }

    /** Secondary text on the chrome (user role, subtitles). */
    public static Color textSoft() {
        return pick(rgb(100, 116, 139), rgb(203, 213, 225), rgb(224, 224, 224));
    }

    /** Icons and glyphs of enabled controls (arrows, pager buttons). */
    public static Color control() {
        return pick(rgb(51, 65, 85), rgb(226, 232, 240), rgb(255, 255, 255));
    }

    /** Disabled controls and quiet labels (section headers). */
    public static Color controlSubtle() {
        return pick(rgb(148, 163, 184), rgb(100, 116, 139), rgb(170, 170, 170));
    }

    /** Soft fill behind chips, hovered rows and progress tracks. */
    public static Color subtleFill() {
        return pick(rgb(241, 245, 249), rgb(30, 41, 59), rgb(40, 40, 40));
    }

    /** Veil painted over a hovered control. */
    public static Color hoverOverlay() {
        return pick(new Color(0, 0, 0, 18), new Color(255, 255, 255, 20), new Color(255, 255, 255, 56));
    }

    /** Veil of a control at rest that must still read as clickable. */
    public static Color restOverlay() {
        return pick(new Color(0, 0, 0, 8), new Color(255, 255, 255, 12), new Color(255, 255, 255, 28));
    }

    static Color pick(Color light, Color dark, Color highContrast) {
        if (UIHelper.isHighContrast()) return highContrast;
        return UIHelper.isLight() ? light : dark;
    }

    private static Color rgb(int r, int g, int b) {
        return new Color(r, g, b);
    }
}

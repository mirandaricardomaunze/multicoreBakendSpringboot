package mz.multicore.erp.gui.components;

import java.awt.Font;

/**
 * The single type scale of the desktop client (docs/TIPOGRAFIA_ESCALA_SPEC.md).
 *
 * <p>Every on-screen text picks one of these roles instead of building its own {@code new Font(...)}.
 * Sizes in between (10, 14, 15, 17, 18, 19, 24, 26, 34) were folded into the nearest role so that two
 * titles of the same kind look the same on every screen. Printed output (shelf labels, volume labels,
 * thermal receipts) is sized for paper and keeps its own fonts; the TV dashboard keeps its own scale.
 * Enforced by {@code TypographyHarnessTest}.
 *
 * <p>The roles follow the interface density (docs/DENSIDADE_ESPACAMENTO_SPEC.md): {@link #applyScale}
 * recomputes them, and the main window is rebuilt so every screen picks up the new sizes. The base sizes
 * below are the "Padrão" density.
 */
public final class Typography {

    static final int BADGE_SIZE = 9;
    static final int KEYCAP_SIZE = 10;
    static final int CAPTION_SIZE = 11;
    static final int BODY_SIZE = 12;
    static final int EMPHASIS_SIZE = 13;
    static final int TITLE_SIZE = 16;
    static final int METRIC_SIZE = 20;
    static final int HERO_SIZE = 30;

    private static float scale = 1f;

    /** Count bubbles on icons (notifications, badges). */
    public static Font BADGE;
    /** Keyboard key caps (F2, Esc, Ctrl+K). */
    public static Font KEYCAP;
    /** Raw text: logs, CSV previews, scale frames. */
    public static Font MONO;

    /** Hints, footers, secondary lines, KPI captions. */
    public static Font CAPTION;
    public static Font CAPTION_BOLD;

    /** Default text: fields, labels, buttons, menus. */
    public static Font BODY;
    public static Font BODY_BOLD;

    /** Reading text and card/section titles: table rows, section headings. */
    public static Font EMPHASIS;
    public static Font EMPHASIS_BOLD;

    /** Dialog and screen titles. */
    public static Font TITLE;
    public static Font TITLE_REGULAR;

    /** Key figures: KPI values, totals in dialogs, page headings. */
    public static Font METRIC;

    /** Figures read from a distance: POS total, live weight. */
    public static Font HERO;

    static {
        applyScale(1f);
    }

    private Typography() {
    }

    /** Recomputes every role for a density factor (0.90 compact, 1.00 standard, 1.15 comfortable). */
    public static synchronized void applyScale(float factor) {
        scale = factor <= 0 ? 1f : factor;
        BADGE = sans(Font.BOLD, BADGE_SIZE);
        KEYCAP = new Font(Font.MONOSPACED, Font.BOLD, size(KEYCAP_SIZE));
        MONO = new Font(Font.MONOSPACED, Font.PLAIN, size(BODY_SIZE));
        CAPTION = sans(Font.PLAIN, CAPTION_SIZE);
        CAPTION_BOLD = sans(Font.BOLD, CAPTION_SIZE);
        BODY = sans(Font.PLAIN, BODY_SIZE);
        BODY_BOLD = sans(Font.BOLD, BODY_SIZE);
        EMPHASIS = sans(Font.PLAIN, EMPHASIS_SIZE);
        EMPHASIS_BOLD = sans(Font.BOLD, EMPHASIS_SIZE);
        TITLE = sans(Font.BOLD, TITLE_SIZE);
        TITLE_REGULAR = sans(Font.PLAIN, TITLE_SIZE);
        METRIC = sans(Font.BOLD, METRIC_SIZE);
        HERO = sans(Font.BOLD, HERO_SIZE);
    }

    public static float scale() {
        return scale;
    }

    /** A base size scaled for the current density, never below 8 px. */
    static int size(int base) {
        return Math.max(8, Math.round(base * scale));
    }

    private static Font sans(int style, int base) {
        return new Font(UIHelper.FONT, style, size(base));
    }
}

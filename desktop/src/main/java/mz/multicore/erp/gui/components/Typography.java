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
 */
public final class Typography {

    /** Count bubbles on icons (notifications, badges). */
    public static final Font BADGE = sans(Font.BOLD, 9);
    /** Keyboard key caps (F2, Esc, Ctrl+K). */
    public static final Font KEYCAP = new Font(Font.MONOSPACED, Font.BOLD, 10);
    /** Raw text: logs, CSV previews, scale frames. */
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /** Hints, footers, secondary lines, KPI captions. */
    public static final Font CAPTION = sans(Font.PLAIN, 11);
    public static final Font CAPTION_BOLD = sans(Font.BOLD, 11);

    /** Default text: fields, labels, buttons, menus. */
    public static final Font BODY = sans(Font.PLAIN, 12);
    public static final Font BODY_BOLD = sans(Font.BOLD, 12);

    /** Reading text and card/section titles: table rows, section headings. */
    public static final Font EMPHASIS = sans(Font.PLAIN, 13);
    public static final Font EMPHASIS_BOLD = sans(Font.BOLD, 13);

    /** Dialog and screen titles. */
    public static final Font TITLE = sans(Font.BOLD, 16);
    public static final Font TITLE_REGULAR = sans(Font.PLAIN, 16);

    /** Key figures: KPI values, totals in dialogs, page headings. */
    public static final Font METRIC = sans(Font.BOLD, 20);

    /** Figures read from a distance: POS total, live weight. */
    public static final Font HERO = sans(Font.BOLD, 30);

    private Typography() {
    }

    private static Font sans(int style, int size) {
        return new Font(UIHelper.FONT, style, size);
    }
}

package mz.multicore.erp.gui.components;

/**
 * The single spacing scale of the desktop client (docs/DENSIDADE_ESPACAMENTO_SPEC.md).
 *
 * <p>Margins and paddings use these steps instead of hand-picked numbers: before there were 154
 * different empty-border combinations, with 10, 12, 14, 15 and 16 px living side by side for the same
 * kind of card. The steps follow the interface density like {@link Typography}: {@link #applyScale}
 * recomputes them and the main window is rebuilt. Enforced by {@code DensitySpacingHarnessTest}.
 */
public final class Spacing {

    static final int[] BASE = {2, 4, 8, 12, 16, 24, 32};

    /** Hairline gap: icon to badge, stacked labels. */
    public static int XXS;
    /** Tight gap inside a control or chip. */
    public static int XS;
    /** Gap between related items; padding of compact controls. */
    public static int SM;
    /** Default padding of rows and form sections. */
    public static int MD;
    /** Padding of cards and dialogs. */
    public static int LG;
    /** Space between page sections. */
    public static int XL;
    /** Large breathing room: empty states, hero blocks. */
    public static int XXL;

    static {
        applyScale(1f);
    }

    private Spacing() {
    }

    public static synchronized void applyScale(float factor) {
        float f = factor <= 0 ? 1f : factor;
        XXS = Math.round(BASE[0] * f);
        XS = Math.round(BASE[1] * f);
        SM = Math.round(BASE[2] * f);
        MD = Math.round(BASE[3] * f);
        LG = Math.round(BASE[4] * f);
        XL = Math.round(BASE[5] * f);
        XXL = Math.round(BASE[6] * f);
    }
}

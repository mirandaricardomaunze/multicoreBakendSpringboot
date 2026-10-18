package mz.multicore.erp.modules.printing;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;

/**
 * Single source of truth for PDF visual styling — fonts, colours, margins.
 * All printable documents pull from here so the brand stays consistent.
 */
public final class PdfTheme {

    public static final Color BRAND = new Color(31, 41, 55);
    public static final Color ACCENT = new Color(139, 92, 246);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color TABLE_HEADER_BG = new Color(248, 250, 252);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color TEXT = new Color(17, 24, 39);
    public static final Color TOTAL_ROW_BG = new Color(245, 247, 250);
    public static final Color ROW_ALT = new Color(249, 250, 251);
    /** Vermelho de anulação — carimbos de documento sem valor (folha de obra anulada). */
    public static final Color DANGER = new Color(185, 28, 28);

    public static final float MARGIN_LEFT = 24f;
    public static final float MARGIN_RIGHT = 24f;
    public static final float MARGIN_TOP = 28f;
    public static final float MARGIN_BOTTOM = 28f;

    public static final String CURRENCY_CODE = "MT";

    private PdfTheme() {}

    public static Font titleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND);
    }

    public static Font subtitleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND);
    }

    public static Font copyBadgeFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, BRAND);
    }

    public static Font bodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT);
    }

    public static Font boldFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT);
    }

    public static Font smallFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    }

    public static Font tableHeaderFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, BRAND);
    }

    public static Font tableBodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 7.8f, TEXT);
    }

    public static Font monoFont() {
        return FontFactory.getFont(FontFactory.COURIER, 9, TEXT);
    }

    /** Carimbo de documento anulado — tem de se ver de longe, numa folha em papel. */
    public static Font voidStampFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, DANGER);
    }
}

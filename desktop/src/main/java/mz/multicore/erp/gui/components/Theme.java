package mz.multicore.erp.gui.components;

import java.awt.Color;

/**
 * Paleta de cores de um tema da interface (claro/escuro). As cores de acento (ACCENT, verdes,
 * vermelhos…) são partilhadas entre temas e vivem em {@link UIHelper}; aqui ficam só as cores
 * sensíveis ao tema: fundos, texto, tabelas e campos.
 *
 * O par {@code DARK}/{@code LIGHT} expõe a paleta na mesma ordem ({@link #palette()}), o que permite
 * re-pintar janelas abertas mapeando, posição a posição, a cor antiga para a nova.
 */
public final class Theme {

    public final String id;
    public final Color bg;            // fundo de página
    public final Color card;          // fundo de cartão/superfície
    public final Color textPrimary;   // texto principal
    public final Color textMuted;     // texto secundário
    public final Color grid;          // linhas de tabela / seleção
    public final Color tableHeaderBg; // cabeçalho de tabela
    public final Color rowAlt;        // linha alternada (zebra)
    public final Color fieldBg;       // fundo de campos de input
    public final Color border;        // bordas de campos/diálogos
    public final Color selectionBg;   // fundo de linha seleccionada

    private Theme(String id, Color bg, Color card, Color textPrimary, Color textMuted, Color grid,
                  Color tableHeaderBg, Color rowAlt, Color fieldBg, Color border, Color selectionBg) {
        this.id = id;
        this.bg = bg;
        this.card = card;
        this.textPrimary = textPrimary;
        this.textMuted = textMuted;
        this.grid = grid;
        this.tableHeaderBg = tableHeaderBg;
        this.rowAlt = rowAlt;
        this.fieldBg = fieldBg;
        this.border = border;
        this.selectionBg = selectionBg;
    }

    /** Cores na ordem canónica — usada para mapear paleta antiga → nova ao re-pintar. */
    public Color[] palette() {
        return new Color[]{bg, card, textPrimary, textMuted, grid, tableHeaderBg, rowAlt, fieldBg, border, selectionBg};
    }

    public static final Theme DARK = new Theme(
            "dark",
            new Color(17, 24, 39),    // bg            Gray-900
            new Color(31, 41, 55),    // card          Gray-800
            new Color(243, 244, 246), // textPrimary   Gray-100
            new Color(156, 163, 175), // textMuted     Gray-400
            new Color(55, 65, 81),    // grid          Gray-700
            new Color(15, 23, 42),    // tableHeaderBg  Slate-900
            new Color(24, 32, 47),    // rowAlt
            new Color(31, 41, 55),    // fieldBg       Gray-800
            new Color(75, 85, 99),    // border        Gray-600
            new Color(55, 65, 81)     // selectionBg   Gray-700
    );

    public static final Theme LIGHT = new Theme(
            "light",
            new Color(243, 244, 246), // bg            Gray-100
            new Color(255, 255, 255), // card          White
            new Color(31, 41, 55),    // textPrimary   Gray-800
            new Color(107, 114, 128), // textMuted     Gray-500
            new Color(226, 232, 240), // grid          Slate-200
            new Color(241, 245, 249), // tableHeaderBg  Slate-100
            new Color(248, 250, 252), // rowAlt        Slate-50
            new Color(255, 255, 255), // fieldBg       White
            new Color(203, 213, 225), // border        Slate-300
            new Color(219, 234, 254)  // selectionBg   Blue-100
    );

    public static final Theme HIGH_CONTRAST = new Theme(
            "high_contrast",
            new Color(0, 0, 0),        // bg            Pure Black (#000000)
            new Color(10, 10, 10),     // card          Deep Black (#0A0A0A)
            new Color(255, 255, 255),  // textPrimary   Pure White (#FFFFFF) - 21:1
            new Color(224, 224, 224),  // textMuted     Silver High-Vis (#E0E0E0) - ~15.8:1
            new Color(80, 80, 80),     // grid          Separator Gray (#505050)
            new Color(18, 18, 18),     // tableHeaderBg Jet Black (#121212)
            new Color(24, 24, 24),     // rowAlt        Zebra (#181818)
            new Color(0, 0, 0),        // fieldBg       Black (#000000)
            new Color(255, 255, 255),  // border        Pure White (#FFFFFF)
            new Color(0, 102, 204)     // selectionBg   Royal Blue (#0066CC)
    );

    public static Theme byId(String id) {
        if (id == null) {
            return DARK;
        }
        String clean = id.trim().toLowerCase();
        if ("light".equals(clean)) {
            return LIGHT;
        }
        if ("high_contrast".equals(clean) || "highcontrast".equals(clean) || "contrast".equals(clean)) {
            return HIGH_CONTRAST;
        }
        return DARK;
    }

    public boolean isHighContrast() {
        return this == HIGH_CONTRAST;
    }

    public boolean isLight() {
        return this == LIGHT;
    }

    public boolean isDark() {
        return this == DARK;
    }
}

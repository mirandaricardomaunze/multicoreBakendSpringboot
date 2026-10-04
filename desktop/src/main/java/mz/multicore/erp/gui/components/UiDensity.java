package mz.multicore.erp.gui.components;

/**
 * Níveis canónicos de densidade e escala da interface gráfica.
 * Modula a altura de linhas de tabelas, campos de input e proporção de elementos visuais.
 */
public enum UiDensity {

    COMPACT("compact", "Compacto (Mais dados)", 28, 32, 0.90f),
    STANDARD("standard", "Padrão (Equilibrado)", 36, 38, 1.00f),
    COMFORTABLE("comfortable", "Confortável (Amplo)", 44, 44, 1.15f);

    private final String id;
    private final String label;
    private final int tableRowHeight;
    private final int formControlHeight;
    private final float fontScale;

    UiDensity(String id, String label, int tableRowHeight, int formControlHeight, float fontScale) {
        this.id = id;
        this.label = label;
        this.tableRowHeight = tableRowHeight;
        this.formControlHeight = formControlHeight;
        this.fontScale = fontScale;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getTableRowHeight() {
        return tableRowHeight;
    }

    public int getFormControlHeight() {
        return formControlHeight;
    }

    public float getFontScale() {
        return fontScale;
    }

    public static UiDensity byId(String id) {
        if (id == null) {
            return STANDARD;
        }
        String clean = id.trim().toLowerCase();
        return switch (clean) {
            case "compact", "compacto", "dense" -> COMPACT;
            case "comfortable", "confortavel", "confortável", "large", "amplo" -> COMFORTABLE;
            default -> STANDARD;
        };
    }
}

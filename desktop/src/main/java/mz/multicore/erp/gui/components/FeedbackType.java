package mz.multicore.erp.gui.components;

import java.awt.Color;

/** Semantica visual comum para feedback contextual, toasts e dialogos. */
public enum FeedbackType {
    SUCCESS("Sucesso", "fas-check-circle", UIHelper.APPROVED_GREEN),
    INFO("Informação", "fas-info-circle", UIHelper.ACCENT_BLUE),
    WARNING("Aviso", "fas-exclamation-triangle", UIHelper.PENDING_YELLOW),
    ERROR("Erro", "fas-exclamation-circle", UIHelper.REJECTED_RED);

    private final String title;
    private final String iconCode;
    private final Color color;

    FeedbackType(String title, String iconCode, Color color) {
        this.title = title;
        this.iconCode = iconCode;
        this.color = color;
    }

    public String title() { return title; }
    public String iconCode() { return iconCode; }
    public Color color() { return color; }
}

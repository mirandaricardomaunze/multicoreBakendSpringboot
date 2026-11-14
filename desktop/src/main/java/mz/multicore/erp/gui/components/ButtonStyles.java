package mz.multicore.erp.gui.components;

import org.kordamp.ikonli.swing.FontIcon;

/**
 * Secondary button looks shared by every table header and dialog: quiet outline, and outline with red text for
 * destructive actions. Only the main action of a header or dialog keeps a solid colour (green).
 */
public final class ButtonStyles {

    private ButtonStyles() {}

    /** Card background, thin border, dark text. Used for every action that is not the main one. */
    static ModernButton outline(ModernButton button) {
        button.setColors(UIHelper.BG_CARD, UIHelper.blendColors(UIHelper.BG_CARD, UIHelper.BORDER, 0.5f));
        button.putClientProperty(ModernButtonPainter.OUTLINE, Boolean.TRUE);
        return button;
    }

    /** Outline with red text and icon: a destructive action stays visible without a solid red block. */
    static ModernButton outlineDanger(ModernButton button) {
        outline(button);
        button.setForeground(UIHelper.REJECTED_RED_HOVER);
        if (button.getIcon() instanceof FontIcon icon) icon.setIconColor(UIHelper.REJECTED_RED_HOVER);
        button.putClientProperty(ModernButtonPainter.DANGER, Boolean.TRUE);
        return button;
    }

    /** The confirm action of a dialog: the only solid (green) button in its footer. */
    public static ModernButton main(ModernButton button) {
        button.setColors(UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER);
        button.putClientProperty(ModernButtonPainter.OUTLINE, null);
        button.putClientProperty(ModernButtonPainter.DANGER, null);
        return button;
    }

    /** Any other dialog button, including "Fechar" and "Voltar": closing is not destructive, so never red. */
    public static ModernButton secondary(ModernButton button) {
        return outline(button);
    }

    /** A dialog action that destroys or refuses something ("Rejeitar"): outline with red text. */
    public static ModernButton destructive(ModernButton button) {
        return outlineDanger(button);
    }

    /** The one "Cancelar" of every dialog: outline, never red (cancelling is not destructive). */
    public static ModernButton cancelButton() {
        ModernButton button = UIHelper.createSecondaryButton("Cancelar");
        button.setIcon(UIHelper.icon("fas-times", 14));
        return outline(button);
    }

    /** Extra footer button of a dialog: outline, with red text when it is destructive (e.g. "Rejeitar"). */
    static ModernButton dialogExtra(ModernButton button) {
        if (isOutline(button)) return button;
        return TableHeaderActions.isDestructive(button) ? outlineDanger(button) : outline(button);
    }

    static boolean isOutline(ModernButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(ModernButtonPainter.OUTLINE));
    }
}

package mz.multicore.erp.gui.components;

import org.kordamp.ikonli.swing.FontIcon;

/** Secondary button looks shared by every table header: quiet outline, and outline for destructive actions. */
final class ButtonStyles {

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

    static boolean isOutline(ModernButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(ModernButtonPainter.OUTLINE));
    }
}

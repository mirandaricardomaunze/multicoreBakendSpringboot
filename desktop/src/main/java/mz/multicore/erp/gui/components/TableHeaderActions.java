package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * One visual hierarchy for the buttons of every table header:
 * <ul>
 *   <li>a single main action in solid green (the last green button the screen gave);</li>
 *   <li>every other button quiet (outline): refresh, menus, blue, yellow or extra green buttons;</li>
 *   <li>destructive (red) buttons stay where the screen put them, as outline with red text and icon,
 *       never as a solid red block next to ordinary actions.</li>
 * </ul>
 */
final class TableHeaderActions {

    private TableHeaderActions() {}

    static JComponent[] arrange(JComponent... actions) {
        if (actions == null) return new JComponent[0];
        List<JComponent> kept = new ArrayList<>();
        ModernButton main = null;
        for (JComponent action : actions) {
            if (action == null) continue;
            kept.add(action);
            if (action instanceof ModernButton button && isMain(button)) main = button;
        }
        for (JComponent action : kept) {
            if (!(action instanceof ModernButton button) || button == main || ButtonStyles.isOutline(button)) continue;
            if (isDestructive(button)) ButtonStyles.outlineDanger(button);
            else ButtonStyles.outline(button);
        }
        return kept.toArray(new JComponent[0]);
    }

    static boolean isDestructive(ModernButton button) {
        return UIHelper.REJECTED_RED.equals(button.getBaseColor());
    }

    static boolean isMain(ModernButton button) {
        return UIHelper.APPROVED_GREEN.equals(button.getBaseColor());
    }
}

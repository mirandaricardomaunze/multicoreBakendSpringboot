package mz.multicore.erp.gui.components;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

/** Cor do cartão: a faixa e o ícone. Vermelho e âmbar só ficam quando há mesmo algo a tratar. */
final class KpiAccent {

    private KpiAccent() {}

    static void install(ModernPanel card, JLabel value, String iconCode, KpiCard.KpiPalette palette) {
        JLabel iconLabel = iconCode == null || iconCode.isBlank() ? null : findIcon(card);
        Runnable apply = () -> {
            Color accent = value != null && KpiCardPalette.isAlert(palette) && isZero(value.getText())
                    ? KpiCardPalette.slatePalette(UIHelper.isLight()).accent : palette.accent;
            card.putClientProperty("kpi.accent", accent);
            if (iconLabel != null) iconLabel.setIcon(UIHelper.icon(iconCode, iconLabel.getIcon().getIconHeight(), accent));
            card.repaint();
        };
        if (value != null) value.addPropertyChangeListener("text", event -> apply.run());
        apply.run();
    }

    static boolean isZero(String text) {
        return text != null && text.contains("0") && text.chars().noneMatch(c -> c >= '1' && c <= '9');
    }

    private static JLabel findIcon(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label) {
                Icon icon = label.getIcon();
                if (icon != null && (label.getText() == null || label.getText().isEmpty())) return label;
            } else if (child instanceof Container nested) {
                JLabel found = findIcon(nested);
                if (found != null) return found;
            }
        }
        return null;
    }
}

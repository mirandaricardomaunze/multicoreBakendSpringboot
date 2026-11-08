package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dashboard header containing the executive-view action and a separate segmented period control.
 * This replaces five individually framed buttons and makes the executive-view action explicit.
 */
final class DashboardPeriodBar {

    static final String TV_LABEL = "Painel Executivo";

    private final Map<DashboardPanel.PeriodFilter, ModernButton> buttons = new EnumMap<>(DashboardPanel.PeriodFilter.class);
    private final JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private final ModernPanel segments = new ModernPanel(UIHelper.RADIUS_MD, idleBackground());

    DashboardPeriodBar(Runnable openTv, Consumer<DashboardPanel.PeriodFilter> onSelect) {
        bar.setOpaque(false);

        ModernButton tv = new ModernButton(TV_LABEL, idleBackground(), idleHover());
        tv.setIcon(UIHelper.icon("fas-tv", 13, UIHelper.ACCENT_BLUE));
        tv.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        tv.setToolTipText("Abrir Painel TV em ecrã inteiro (F11)");
        tv.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1, true), BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        tv.addActionListener(e -> openTv.run());
        bar.add(tv);
        bar.add(Box.createHorizontalStrut(16));

        segments.putClientProperty("card.border", UIHelper.BORDER);
        segments.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        segments.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        for (DashboardPanel.PeriodFilter period : DashboardPanel.PeriodFilter.values()) {
            ModernButton button = new ModernButton(period.getLabel(), idleBackground(), idleHover());
            button.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
            button.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
            button.setPreferredSize(new Dimension(button.getPreferredSize().width, 28));
            button.addActionListener(e -> onSelect.accept(period));
            buttons.put(period, button);
            segments.add(button);
        }
        bar.add(segments);
    }

    JPanel component() {
        return bar;
    }

    /** Colours only the active period while inactive options blend into the control background. */
    void select(DashboardPanel.PeriodFilter current) {
        buttons.forEach((period, button) -> {
            if (period == current) {
                button.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
            } else {
                button.setColors(idleBackground(), idleHover());
            }
        });
    }

    private static Color idleBackground() {
        return UIHelper.BG_CARD;
    }

    private static Color idleHover() {
        return UIHelper.isLight() ? UIHelper.ROW_ALT : UIHelper.GRID;
    }
}

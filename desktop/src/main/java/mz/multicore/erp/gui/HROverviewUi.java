package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Componentes visuais puros do resumo de Recursos Humanos. */
final class HROverviewUi {
    private HROverviewUi() {}

    static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }

    static ModernPanel chartCard(SimpleBarChart chart) {
        ModernPanel card = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }
}

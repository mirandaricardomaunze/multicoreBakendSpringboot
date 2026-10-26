package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Aba de declarações fiscais e documentos para a Autoridade Tributária. */
final class FiscalDeclarationsTab {

    private final FiscalIvaTab ivaTab;

    FiscalDeclarationsTab(FiscalIvaTab ivaTab) {
        this.ivaTab = ivaTab;
    }

    public JPanel buildTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Documentos para a Autoridade Tributária");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);

        JLabel hint = new JLabel("<html>Estes documentos são exportados em PDF profissional, prontos para impressão e arquivo. " +
                "Inclua todas as faturas aprovadas/pagas e compras não-canceladas do período selecionado.</html>");
        hint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        hint.setForeground(UIHelper.TEXT_MUTED);

        ModernButton ivaDocBtn = UIHelper.createSecondaryButton("Declaração Mensal de IVA");
        ivaDocBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        ivaDocBtn.setPreferredSize(new Dimension(280, 44));
        ivaDocBtn.addActionListener(e -> ivaTab.printIvaDeclaration());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actions.setOpaque(false);
        actions.add(ivaDocBtn);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(hint);
        content.add(Box.createRigidArea(new Dimension(0, 18)));
        content.add(actions);

        card.add(content, BorderLayout.NORTH);
        tab.add(card, BorderLayout.NORTH);
        return tab;
    }
}

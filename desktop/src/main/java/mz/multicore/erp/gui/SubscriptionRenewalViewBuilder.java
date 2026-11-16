package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DialogChrome;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

final class SubscriptionRenewalViewBuilder {

    private SubscriptionRenewalViewBuilder() {}

    static ModernPanel buildTotalCard(JLabel discountBadge, JLabel totalLabel) {
        ModernPanel totalCard = new ModernPanel(12);
        totalCard.setBackground(UIHelper.ROW_ALT);
        totalCard.setBorder(new EmptyBorder(Spacing.MD, Spacing.LG, Spacing.MD, Spacing.LG));
        totalCard.setLayout(new BorderLayout(8, 2));
        totalCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel totalLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        totalLeft.setOpaque(false);
        JLabel totalTitle = new JLabel("VALOR TOTAL:");
        totalTitle.setFont(Typography.BODY_BOLD);
        totalTitle.setForeground(UIHelper.TEXT_MUTED);
        discountBadge.setFont(Typography.CAPTION_BOLD);
        discountBadge.setForeground(UIHelper.APPROVED_GREEN);
        totalLeft.add(totalTitle);
        totalLeft.add(discountBadge);

        totalLabel.setFont(Typography.METRIC);
        totalLabel.setForeground(UIHelper.ACCENT_BLUE);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        totalCard.add(totalLeft, BorderLayout.WEST);
        totalCard.add(totalLabel, BorderLayout.EAST);
        return totalCard;
    }

    static JPanel buildRadioPanel(JRadioButton mpesaRadio, JRadioButton emolaRadio, JRadioButton bankRadio) {
        ButtonGroup group = new ButtonGroup();
        Font radioFont = Typography.BODY;
        for (JRadioButton rb : new JRadioButton[]{mpesaRadio, emolaRadio, bankRadio}) {
            rb.setOpaque(false);
            rb.setFont(radioFont);
            group.add(rb);
        }
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        radioPanel.setOpaque(false);
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioPanel.add(mpesaRadio);
        radioPanel.add(emolaRadio);
        radioPanel.add(bankRadio);
        return radioPanel;
    }

    static ModernPanel buildMobileCard(JTextField phoneField) {
        ModernPanel mobileCard = new ModernPanel(10);
        mobileCard.setBackground(UIHelper.ROW_ALT);
        mobileCard.setBorder(new EmptyBorder(Spacing.MD, Spacing.LG, Spacing.MD, Spacing.LG));
        mobileCard.setLayout(new BorderLayout(8, 6));
        mobileCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel phonePrompt = new JLabel("Número de Telemóvel para receber o prompt de PIN:");
        phonePrompt.setFont(Typography.CAPTION_BOLD);
        phonePrompt.setForeground(UIHelper.TEXT_MUTED);

        UIHelper.styleTextField(phoneField);
        phoneField.setFont(Typography.EMPHASIS_BOLD);
        phoneField.putClientProperty("JTextField.placeholderText", "Ex.: 84 123 4567 ou 86 123 4567");

        mobileCard.add(phonePrompt, BorderLayout.NORTH);
        mobileCard.add(phoneField, BorderLayout.CENTER);
        // In the dialog's vertical box the card would stretch and make the field ~80 px tall.
        mobileCard.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, mobileCard.getPreferredSize().height));
        return mobileCard;
    }

    static ModernPanel buildBankCard(JTextField refField, JTextArea notesArea) {
        ModernPanel bankCard = new ModernPanel(10);
        bankCard.setBackground(UIHelper.ROW_ALT);
        bankCard.setBorder(new EmptyBorder(Spacing.MD, Spacing.LG, Spacing.MD, Spacing.LG));
        bankCard.setLayout(new BoxLayout(bankCard, BoxLayout.Y_AXIS));
        bankCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        bankCard.setVisible(false);

        JLabel bankPrompt = new JLabel("Coordenadas Bancárias da Plataforma: Millennium BIM: 123456789 | BCI: 987654321");
        bankPrompt.setFont(Typography.CAPTION);
        bankPrompt.setForeground(UIHelper.TEXT_MUTED);

        UIHelper.styleTextField(refField);
        refField.setFont(Typography.BODY);
        refField.putClientProperty("JTextField.placeholderText", "Nº de Comprovativo ou Código de Depósito");

        notesArea.setFont(Typography.BODY);
        notesArea.setLineWrap(true);

        bankCard.add(bankPrompt);
        bankCard.add(Box.createRigidArea(new Dimension(0, 6)));
        bankCard.add(new JLabel("Referência do Pagamento:"));
        bankCard.add(refField);
        bankCard.add(Box.createRigidArea(new Dimension(0, 6)));
        bankCard.add(new JLabel("Observações adicionais:"));
        bankCard.add(new JScrollPane(notesArea));
        return bankCard;
    }

    static JPanel buildFooter(Runnable onCancel, ModernButton submitBtn) {
        ModernButton cancelBtn = ButtonStyles.cancelButton();
        cancelBtn.addActionListener(e -> onCancel.run());
        return DialogChrome.footer(cancelBtn, submitBtn);
    }
}

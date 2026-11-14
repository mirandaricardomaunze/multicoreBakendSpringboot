package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

final class SubscriptionRenewalViewBuilder {

    private SubscriptionRenewalViewBuilder() {}

    static JPanel buildHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(12, 4));
        headerPanel.setOpaque(false);
        headerPanel.add(new JLabel(UIHelper.icon("fas-crown", 28, UIHelper.PENDING_YELLOW)), BorderLayout.WEST);

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 2));
        headerText.setOpaque(false);
        JLabel titleLabel = new JLabel("Activar ou Renovar Plano do Sistema");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);

        JLabel subtitleLabel = new JLabel("Seleccione o seu plano comercial e efectue o pagamento.");
        subtitleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitleLabel.setForeground(UIHelper.TEXT_MUTED);

        headerText.add(titleLabel);
        headerText.add(subtitleLabel);
        headerPanel.add(headerText, BorderLayout.CENTER);
        return headerPanel;
    }

    static ModernPanel buildTotalCard(JLabel discountBadge, JLabel totalLabel) {
        ModernPanel totalCard = new ModernPanel(12);
        totalCard.setBackground(UIHelper.ROW_ALT);
        totalCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        totalCard.setLayout(new BorderLayout(8, 2));
        totalCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel totalLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        totalLeft.setOpaque(false);
        JLabel totalTitle = new JLabel("VALOR TOTAL:");
        totalTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        totalTitle.setForeground(UIHelper.TEXT_MUTED);
        discountBadge.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        discountBadge.setForeground(UIHelper.APPROVED_GREEN);
        totalLeft.add(totalTitle);
        totalLeft.add(discountBadge);

        totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        totalLabel.setForeground(UIHelper.ACCENT_BLUE);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        totalCard.add(totalLeft, BorderLayout.WEST);
        totalCard.add(totalLabel, BorderLayout.EAST);
        return totalCard;
    }

    static JPanel buildRadioPanel(JRadioButton mpesaRadio, JRadioButton emolaRadio, JRadioButton bankRadio) {
        ButtonGroup group = new ButtonGroup();
        Font radioFont = new Font(UIHelper.FONT, Font.PLAIN, 12);
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
        mobileCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        mobileCard.setLayout(new BorderLayout(8, 6));
        mobileCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel phonePrompt = new JLabel("Número de Telemóvel para receber o prompt de PIN:");
        phonePrompt.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        phonePrompt.setForeground(UIHelper.TEXT_MUTED);

        UIHelper.styleTextField(phoneField);
        phoneField.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        phoneField.putClientProperty("JTextField.placeholderText", "Ex.: 84 123 4567 ou 86 123 4567");

        mobileCard.add(phonePrompt, BorderLayout.NORTH);
        mobileCard.add(phoneField, BorderLayout.CENTER);
        return mobileCard;
    }

    static ModernPanel buildBankCard(JTextField refField, JTextArea notesArea) {
        ModernPanel bankCard = new ModernPanel(10);
        bankCard.setBackground(UIHelper.ROW_ALT);
        bankCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        bankCard.setLayout(new BoxLayout(bankCard, BoxLayout.Y_AXIS));
        bankCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        bankCard.setVisible(false);

        JLabel bankPrompt = new JLabel("Coordenadas Bancárias da Plataforma: Millennium BIM: 123456789 | BCI: 987654321");
        bankPrompt.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        bankPrompt.setForeground(UIHelper.TEXT_MUTED);

        UIHelper.styleTextField(refField);
        refField.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        refField.putClientProperty("JTextField.placeholderText", "Nº de Comprovativo ou Código de Depósito");

        notesArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
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
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        ModernButton cancelBtn = ButtonStyles.cancelButton();
        cancelBtn.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        cancelBtn.addActionListener(e -> onCancel.run());
        footer.add(cancelBtn);
        footer.add(submitBtn);
        return footer;
    }
}

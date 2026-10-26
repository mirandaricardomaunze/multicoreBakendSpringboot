package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Painel de configuração e validação de pagamento móvel (operador e telemóvel).
 */
public final class MobilePaymentInitiationPanel extends JPanel {

    private final JComboBox<MobilePaymentProvider> providerCombo;
    private final JTextField phoneField;
    private final JLabel validationLabel;
    private final ModernButton sendButton;

    public MobilePaymentInitiationPanel(BigDecimal amount, MobilePaymentProvider initialProvider, Runnable onSend) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        ModernPanel amountCard = new ModernPanel(10);
        amountCard.setBackground(UIHelper.ROW_ALT);
        amountCard.setBorder(new EmptyBorder(10, 16, 10, 16));
        amountCard.setLayout(new BorderLayout(8, 2));
        JLabel amountTitle = new JLabel("TOTAL A COBRAR:");
        amountTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        amountTitle.setForeground(UIHelper.TEXT_MUTED);
        JLabel amountVal = new JLabel(String.format("%,.2f MT", amount != null ? amount : BigDecimal.ZERO));
        amountVal.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
        amountVal.setForeground(UIHelper.ACCENT_BLUE);
        amountVal.setHorizontalAlignment(SwingConstants.RIGHT);
        amountCard.add(amountTitle, BorderLayout.WEST); amountCard.add(amountVal, BorderLayout.EAST);
        amountCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(amountCard); add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel providerLabel = new JLabel("Operador de Pagamento:");
        providerLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        providerLabel.setForeground(UIHelper.TEXT_LIGHT);
        providerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(providerLabel); add(Box.createRigidArea(new Dimension(0, 4)));

        providerCombo = new JComboBox<>(MobilePaymentProvider.values());
        providerCombo.setSelectedItem(initialProvider != null ? initialProvider : MobilePaymentProvider.MPESA);
        UIHelper.styleComboBox(providerCombo);
        providerCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        providerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        providerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MobilePaymentProvider p) {
                    setText(p.getDisplayName() + " (" + p.getOperator() + " - " + p.getPrefixes() + ")");
                }
                return this;
            }
        });
        add(providerCombo);
        add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel phoneLabel = new JLabel("Número de Telemóvel:");
        phoneLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        phoneLabel.setForeground(UIHelper.TEXT_LIGHT);
        phoneLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(phoneLabel);
        add(Box.createRigidArea(new Dimension(0, 4)));

        phoneField = new JTextField();
        UIHelper.styleTextField(phoneField);
        phoneField.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        phoneField.putClientProperty("JTextField.placeholderText", "Ex.: 84 123 4567 ou 86 123 4567");
        phoneField.setAlignmentX(Component.LEFT_ALIGNMENT);
        phoneField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        add(phoneField);

        validationLabel = new JLabel("Introduza o número de 9 dígitos");
        validationLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        validationLabel.setForeground(UIHelper.TEXT_MUTED);
        validationLabel.setBorder(new EmptyBorder(3, 2, 8, 2));
        validationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(validationLabel);

        sendButton = UIHelper.createPrimaryButton("Enviar Pedido ao Telemóvel (Push USSD)");
        sendButton.setIcon(UIHelper.icon("fas-paper-plane", 13, Color.WHITE));
        sendButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        sendButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        sendButton.setPreferredSize(new Dimension(0, 40));
        sendButton.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> { if (onSend != null) onSend.run(); });
        add(sendButton);

        Runnable validateInput = this::validateForm;
        providerCombo.addActionListener(e -> validateInput.run());
        UIHelper.onTextChange(phoneField, validateInput);
    }

    private void validateForm() {
        MobilePaymentProvider prov = (MobilePaymentProvider) providerCombo.getSelectedItem();
        String digits = phoneField.getText().replaceAll("[^0-9]", "");
        if (digits.startsWith("258") && digits.length() == 12) digits = digits.substring(3);

        if (digits.isEmpty()) {
            validationLabel.setIcon(null); validationLabel.setText("Introduza o número de 9 dígitos");
            validationLabel.setForeground(UIHelper.TEXT_MUTED); sendButton.setEnabled(false);
            return;
        }

        if (digits.length() == 9) {
            String prefix = digits.substring(0, 2);
            boolean valid = (prov == MobilePaymentProvider.MPESA && (prefix.equals("84") || prefix.equals("85")))
                    || (prov == MobilePaymentProvider.EMOLA && (prefix.equals("86") || prefix.equals("87")));
            if (valid) {
                validationLabel.setIcon(UIHelper.icon("fas-check-circle", 13, UIHelper.APPROVED_GREEN));
                validationLabel.setText("Número " + prov.getOperator() + " válido (" + digits + ")");
                validationLabel.setForeground(UIHelper.APPROVED_GREEN); sendButton.setEnabled(true);
            } else {
                validationLabel.setIcon(UIHelper.icon("fas-exclamation-triangle", 13, UIHelper.REJECTED_RED));
                validationLabel.setText("Prefixo " + prefix + " incompatível com " + prov.getDisplayName() + " (" + prov.getPrefixes() + ")");
                validationLabel.setForeground(UIHelper.REJECTED_RED); sendButton.setEnabled(false);
            }
        } else {
            validationLabel.setIcon(null);
            validationLabel.setText("Faltam " + (9 - digits.length()) + " dígitos (" + digits.length() + "/9)");
            validationLabel.setForeground(UIHelper.TEXT_MUTED); sendButton.setEnabled(false);
        }
    }

    public MobilePaymentProvider getSelectedProvider() { return (MobilePaymentProvider) providerCombo.getSelectedItem(); }
    public String getPhoneNumber() { return phoneField.getText().trim(); }

    public void setControlsEnabled(boolean enabled) {
        providerCombo.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        if (enabled) validateForm(); else sendButton.setEnabled(false);
    }
}

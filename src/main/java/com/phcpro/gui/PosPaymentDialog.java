package com.phcpro.gui;

import com.phcpro.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/** Recolhe e valida o pagamento do checkout sem executar regras comerciais. */
final class PosPaymentDialog {
    private PosPaymentDialog() {}

    static com.phcpro.modules.pos.dto.PosPaymentRequest show(BigDecimal total, Long accountId) {
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Numerário", "Cartão", "Transferência", "M-Pesa", "e-Mola"});
        UIHelper.styleComboBox(methodCombo);

        JTextField totalField = new JTextField(String.format("%,.2f MT", total));
        UIHelper.styleTextField(totalField);
        totalField.setEditable(false);

        JTextField tenderedField = new JTextField(total.toPlainString());
        UIHelper.styleTextField(tenderedField);

        // Referência da transação (nº autorização cartão, comprovativo, ID M-Pesa/e-Mola).
        JTextField refField = new JTextField();
        UIHelper.styleTextField(refField);
        refField.putClientProperty("JTextField.placeholderText", "ID/comprovativo (M-Pesa, e-Mola, cartão)…");

        JLabel changeLabel = new JLabel("Troco: 0,00 MT");
        changeLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        changeLabel.setForeground(UIHelper.APPROVED_GREEN);

        JPanel quickCashPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quickCashPanel.setOpaque(false);
        addTenderButton(quickCashPanel, "Exacto", total, tenderedField);
        for (int denomination : new int[]{100, 200, 500, 1000}) {
            addTenderButton(quickCashPanel, String.valueOf(denomination),
                    BigDecimal.valueOf(denomination), tenderedField);
        }
        JLabel quickLabel = new JLabel("Recebimento rápido");
        quickLabel.setForeground(UIHelper.TEXT_MUTED);
        quickLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        JPanel quickCashSection = new JPanel();
        quickCashSection.setLayout(new BoxLayout(quickCashSection, BoxLayout.Y_AXIS));
        quickCashSection.setOpaque(false);
        quickLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickCashPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickCashSection.add(quickLabel);
        quickCashSection.add(Box.createRigidArea(new Dimension(0, 6)));
        quickCashSection.add(quickCashPanel);

        // Recalcula o troco a cada alteração; só relevante quando o método é numerário.
        Runnable recompute = () -> {
            boolean cash = methodCombo.getSelectedIndex() == 0;
            tenderedField.setEnabled(cash);
            quickCashSection.setVisible(cash);
            refField.setEnabled(!cash); // referência só faz sentido em métodos electrónicos
            if (!cash) {
                changeLabel.setText("Troco: —");
                return;
            }
            try {
                BigDecimal tendered = new BigDecimal(tenderedField.getText().trim().replace(",", "."));
                BigDecimal change = tendered.subtract(total);
                if (change.compareTo(BigDecimal.ZERO) < 0) {
                    changeLabel.setForeground(UIHelper.PENDING_YELLOW);
                    changeLabel.setText(String.format("Falta: %,.2f MT", change.abs()));
                } else {
                    changeLabel.setForeground(UIHelper.APPROVED_GREEN);
                    changeLabel.setText(String.format("Troco: %,.2f MT", change));
                }
            } catch (NumberFormatException ex) {
                changeLabel.setForeground(UIHelper.PENDING_YELLOW);
                changeLabel.setText("Troco: valor inválido");
            }
        };
        methodCombo.addActionListener(e -> recompute.run());
        UIHelper.onTextChange(tenderedField, recompute);
        recompute.run();

        JPanel form = UIHelper.createDialogForm(
                "Método de pagamento:", methodCombo,
                "Total a pagar:", totalField,
                "Valor entregue (MT):", tenderedField,
                "Referência:", refField
        );
        changeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        changeLabel.setBorder(new EmptyBorder(8, 4, 0, 4));
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(form, BorderLayout.CENTER);
        JPanel paymentFooter = new JPanel();
        paymentFooter.setLayout(new BoxLayout(paymentFooter, BoxLayout.Y_AXIS));
        paymentFooter.setOpaque(false);
        changeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickCashSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        paymentFooter.add(quickCashSection);
        paymentFooter.add(changeLabel);
        panel.add(paymentFooter, BorderLayout.SOUTH);

        // Modal premium (ModernFormDialog): ícone + subtítulo + botão estilizado. A validação corre no
        // onSave — lançar excepção mantém o modal aberto (sem recursão).
        com.phcpro.modules.pos.dto.PosPaymentRequest[] result = {null};
        boolean ok = new ModernFormDialog(UIHelper.mainWindow, "Pagamento", "fas-money-bill-wave",
                "Receba do cliente e confirme o troco", panel)
                .setConfirmButton("Confirmar Pagamento", "fas-check")
                .setOnSave(() -> {
                    int methodIdx = methodCombo.getSelectedIndex();
                    if (methodIdx == 0) {
                        // Numerário: valida valor entregue ≥ total; entra na gaveta (sem conta).
                        BigDecimal tendered;
                        try {
                            tendered = new BigDecimal(tenderedField.getText().trim().replace(",", "."));
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Valor entregue inválido.");
                        }
                        if (tendered.compareTo(total) < 0) {
                            throw new IllegalArgumentException("O valor entregue é inferior ao total a pagar.");
                        }
                        result[0] = new com.phcpro.modules.pos.dto.PosPaymentRequest("CASH", total, tendered, null, null);
                    } else {
                        // 1=Cartão, 2=Transferência, 3=M-Pesa, 4=e-Mola — todos electrónicos (tesouraria).
                        String[] methods = {"CASH", "CARD", "BANK_TRANSFER", "MPESA", "EMOLA"};
                        String method = methods[methodIdx];
                        String ref = refField.getText().trim();
                        result[0] = new com.phcpro.modules.pos.dto.PosPaymentRequest(
                                method, total, total, ref.isEmpty() ? null : ref, accountId);
                    }
                })
                .showDialog();
        return ok ? result[0] : null;
    }

    private static void addTenderButton(JPanel parent, String label, BigDecimal value, JTextField target) {
        ModernButton button = UIHelper.createSecondaryButton(label);
        button.setPreferredSize(new Dimension("Exacto".equals(label) ? 78 : 64, 38));
        button.addActionListener(e -> target.setText(value.stripTrailingZeros().toPlainString()));
        parent.add(button);
    }

}


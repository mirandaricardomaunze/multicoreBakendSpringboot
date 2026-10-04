package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;

/**
 * Diálogo canónico para registo de Sangrias e Suprimentos de Caixa no POS.
 */
public class PosCashMovementDialog {

    @FunctionalInterface
    public interface CashMovementCallback {
        void onConfirm(String type, BigDecimal amount, String description);
    }

    public static void show(Window parent, Long sessionId, CashMovementCallback callback) {
        JPanel form = new JPanel(new BorderLayout(0, 16));
        form.setOpaque(false);

        // Seletor de Tipo (Suprimento / Sangria)
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        typePanel.setOpaque(false);

        JRadioButton rbSuprimento = new JRadioButton("SUPRIMENTO (Entrada de Troco)");
        JRadioButton rbSangria = new JRadioButton("SANGRIA (Retirada para Cofre)");
        rbSuprimento.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        rbSangria.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        rbSuprimento.setForeground(UIHelper.APPROVED_GREEN);
        rbSangria.setForeground(UIHelper.PENDING_YELLOW);
        rbSuprimento.setOpaque(false);
        rbSangria.setOpaque(false);
        rbSuprimento.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(rbSuprimento);
        group.add(rbSangria);
        typePanel.add(rbSuprimento);
        typePanel.add(rbSangria);
        form.add(typePanel, BorderLayout.NORTH);

        // Campos de valor e motivo
        MoneyField amountField = new MoneyField();
        JTextField descField = new JTextField();
        descField.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));

        JPanel fieldsPanel = new JPanel(new GridLayout(4, 1, 0, 6));
        fieldsPanel.setOpaque(false);

        JLabel lblAmount = new JLabel("Valor do Movimento (MT):");
        lblAmount.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lblAmount.setForeground(UIHelper.TEXT_LIGHT);

        JLabel lblDesc = new JLabel("Motivo / Justificação Obrigatória:");
        lblDesc.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lblDesc.setForeground(UIHelper.TEXT_LIGHT);

        fieldsPanel.add(lblAmount);
        fieldsPanel.add(amountField);
        fieldsPanel.add(lblDesc);
        fieldsPanel.add(descField);

        form.add(fieldsPanel, BorderLayout.CENTER);

        String title = "Movimento de Caixa — Sessão #" + (sessionId != null ? sessionId : "Activa");
        ModernFormDialog dlg = new ModernFormDialog(
                parent,
                title,
                "fas-money-bill-wave",
                "Registo auditável de entrada de trocos ou retirada de numerário da gaveta.",
                form
        );

        dlg.setOnSave(() -> {
            BigDecimal amt = amountField.value();
            if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                amountField.requestFocusInWindow();
                throw new IllegalArgumentException("O valor do movimento deve ser maior do que zero.");
            }

            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                descField.requestFocusInWindow();
                throw new IllegalArgumentException("Indique o motivo ou justificação do movimento de caixa.");
            }

            String type = rbSuprimento.isSelected() ? "SUPRIMENTO" : "SANGRIA";
            callback.onConfirm(type, amt, desc);
        });

        dlg.showDialog();
    }
}

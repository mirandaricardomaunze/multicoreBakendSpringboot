package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

public final class CreditExceptionDialog {

    private CreditExceptionDialog() {}

    public static void show(Component parent, CreditRiskApiClient client, ClientCreditRiskDTO selected, Runnable onSuccess) {
        Window ancestor = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog((Frame) (ancestor instanceof Frame ? ancestor : null), "Pedido de Excepção de Crédito", true);
        dialog.setLayout(new BorderLayout(0, 16));
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Cliente
        content.add(createFormLabel("Cliente:"));
        JTextField clientField = new JTextField(selected != null ? selected.clientName() + " (NUIT: " + selected.clientTaxId() + ")" : "Seleccione um cliente na tabela primeiro");
        clientField.setEditable(false);
        clientField.setPreferredSize(new Dimension(420, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleTextField(clientField);
        content.add(clientField);
        content.add(Box.createVerticalStrut(12));

        // Valor Solicitado
        content.add(createFormLabel("Valor de Crédito Adicional Requerido (MT):"));
        MoneyField amountField = new MoneyField();
        amountField.setPreferredSize(new Dimension(420, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleTextField(amountField);
        content.add(amountField);
        content.add(Box.createVerticalStrut(12));

        // Motivo / Justificação
        content.add(createFormLabel("Justificação Comercial / Motivo da Excepção:"));
        JTextArea reasonArea = new JTextArea(4, 30);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setBackground(UIHelper.BG_CARD);
        reasonArea.setForeground(UIHelper.TEXT_LIGHT);
        reasonArea.setCaretColor(UIHelper.TEXT_LIGHT);
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        UIHelper.styleScrollPane(reasonScroll);
        content.add(reasonScroll);

        dialog.add(content, BorderLayout.CENTER);

        // Botões do diálogo
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        ModernButton cancelBtn = UIHelper.createSecondaryButton("Cancelar");
        cancelBtn.setPreferredSize(new Dimension(100, UIHelper.FORM_CONTROL_HEIGHT));
        cancelBtn.addActionListener(e -> dialog.dispose());

        ModernButton submitBtn = UIHelper.createSuccessButton("Submeter à Gerência");
        submitBtn.setIcon(UIHelper.icon("fas-paper-plane", 14));
        submitBtn.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));

        submitBtn.addActionListener(e -> {
            if (selected == null) {
                ToastManager.show(dialog, FeedbackType.WARNING, "Por favor seleccione primeiro um cliente na tabela.");
                return;
            }
            BigDecimal amount;
            try {
                amount = amountField.value();
            } catch (Exception ex) {
                ToastManager.show(dialog, FeedbackType.WARNING, "Introduza um valor monetário válido.");
                return;
            }
            if (amount == null || amount.signum() <= 0) {
                ToastManager.show(dialog, FeedbackType.WARNING, "Introduza um valor válido para o crédito.");
                return;
            }
            String reason = reasonArea.getText().trim();
            if (reason.length() < 10) {
                ToastManager.show(dialog, FeedbackType.WARNING, "A justificação deve conter pelo menos 10 caracteres explicativos.");
                return;
            }

            submitBtn.setEnabled(false);
            CreditExceptionApprovalRequest req = new CreditExceptionApprovalRequest(
                    selected.clientId(), amount, reason, CurrentUserContext.getUsername()
            );

            new SwingWorker<ApprovalRequestDTO, Void>() {
                @Override
                protected ApprovalRequestDTO doInBackground() {
                    return client.requestCreditException(req);
                }

                @Override
                protected void done() {
                    dialog.dispose();
                    try {
                        ApprovalRequestDTO approval = get();
                        ToastManager.success(parent,
                                "Pedido de excepção submetido com sucesso. ID: #" + approval.id() + " (Perfil: " + approval.requiredRole() + ")");
                        if (onSuccess != null) onSuccess.run();
                    } catch (Exception ex) {
                        ToastManager.show(parent, FeedbackType.ERROR,
                                "Erro ao submeter excepção: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        buttons.add(cancelBtn);
        buttons.add(submitBtn);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.pack();
        UIHelper.applyStandardModalBounds(dialog, 480, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setBorder(new EmptyBorder(0, 0, 4, 0));
        return label;
    }
}

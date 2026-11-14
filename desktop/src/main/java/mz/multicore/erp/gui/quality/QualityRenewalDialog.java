package mz.multicore.erp.gui.quality;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public final class QualityRenewalDialog {

    private QualityRenewalDialog() {}

    public static void show(Component parent, QualityApiClient client, Long assignmentId, Long companyId, Runnable onComplete) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(window, "Renovar Atribuição de Equipamento", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(0, 10));

        ModernPanel card = new ModernPanel(14);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        DateField renewalDateField = new DateField(LocalDate.now());
        JTextField notesField = new JTextField("Renovação periódica de EPI");
        UIHelper.styleTextField(notesField);
        JCheckBox printCheck = new JCheckBox("Imprimir Termo de Entrega atualizado", true);
        printCheck.setOpaque(false);

        g.gridx = 0; g.gridy = 0; g.weightx = 0.35;
        card.add(new JLabel("Nova Data de Entrega:"), g);
        g.gridx = 1; g.weightx = 0.65;
        card.add(renewalDateField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0.35;
        card.add(new JLabel("Observações / Motivo:"), g);
        g.gridx = 1; g.weightx = 0.65;
        card.add(notesField, g);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        card.add(printCheck, g);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UIHelper.BG_DARK);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.BORDER));

        ModernButton cancelBtn = ButtonStyles.cancelButton();
        cancelBtn.addActionListener(e -> dialog.dispose());
        ModernButton confirmBtn = UIHelper.createPrimaryButton("Confirmar Renovação");

        confirmBtn.addActionListener(e -> {
            LocalDate newDate = renewalDateField.getDate();
            if (newDate == null) {
                ToastManager.show(dialog, FeedbackType.WARNING, "Selecione uma data válida no calendário.");
                renewalDateField.requestFocus();
                return;
            }
            String notes = notesField.getText().trim();
            boolean print = printCheck.isSelected();

            UIHelper.runWithProgress(dialog, "A renovar equipamento...", () -> {
                return client.renewAssignment(assignmentId, companyId, newDate, notes);
            }, renewed -> {
                dialog.dispose();
                ToastManager.show(parent, FeedbackType.SUCCESS, "Equipamento renovado com sucesso!");
                if (onComplete != null) onComplete.run();
                if (print && client != null) {
                    QualityPrintHelper.printDeliverySheet(parent, client, renewed.id(), companyId);
                }
            }, err -> ToastManager.show(dialog, FeedbackType.ERROR, "Erro ao renovar: " + err.getMessage()));
        });

        footer.add(cancelBtn);
        footer.add(confirmBtn);

        dialog.add(card, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        UIHelper.applyStandardModalBounds(dialog, 480, 260);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}

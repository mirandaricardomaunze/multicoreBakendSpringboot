package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Shows a new key's secret once (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.1): read-only field, "Copiar" to the
 * clipboard and the warning; when closed the secret is cleared from the screen and never kept.
 */
final class ApiKeySecretDialog {

    static final String WARNING = "Esta chave não volta a ser mostrada. Guarde-a agora.";

    private final JTextField secretField = new JTextField();
    private final ModernButton copyBtn = UIHelper.createSecondaryButton("Copiar");
    private final ModernFormDialog dialog;

    ApiKeySecretDialog(String secret) {
        secretField.setText(secret);
        secretField.setEditable(false);
        UIHelper.styleTextField(secretField);

        JLabel warning = new JLabel(WARNING);
        warning.setFont(Typography.CAPTION);
        warning.setForeground(UIHelper.PENDING_YELLOW);
        warning.setIcon(UIHelper.icon("fas-exclamation-triangle", 14, UIHelper.PENDING_YELLOW));

        copyBtn.setIcon(UIHelper.icon("fas-copy", 14));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(secretField.getText()), null);
            ToastManager.show(secretField, FeedbackType.SUCCESS, "Chave copiada para a área de transferência.");
        });

        JPanel content = new JPanel(new BorderLayout(0, Spacing.SM));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(Spacing.SM, 0, Spacing.SM, 0));
        content.add(secretField, BorderLayout.CENTER);
        content.add(warning, BorderLayout.SOUTH);

        dialog = new ModernFormDialog(UIHelper.mainWindow, "Chave de API criada", "fas-key",
                "Copie a chave e configure-a no sistema externo", content)
                .asReadOnly("Fechar")
                .addActionButton(copyBtn);
    }

    /** Shows the secret until the user closes the dialog, then discards it. */
    void show() {
        try {
            dialog.showDialog();
        } finally {
            discard();
        }
    }

    void discard() {
        secretField.setText("");
    }

    JTextField secretField() { return secretField; }
    String warningText() { return WARNING; }
    ModernButton copyButton() { return copyBtn; }
}

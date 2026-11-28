package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Shows a new secret once — an API key (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.1) or a webhook's {@code whsec_}
 * (§4.4.2): read-only field, "Copiar" to the clipboard and the warning; when closed the secret is cleared from the
 * screen and never kept.
 */
final class ApiKeySecretDialog {

    static final String WARNING = "Esta chave não volta a ser mostrada. Guarde-a agora.";
    static final String WEBHOOK_WARNING = "Este segredo não volta a ser mostrado. Guarde-o agora para validar a "
            + "assinatura X-Multicore-Signature.";

    private final JTextField secretField = new JTextField();
    private final ModernButton copyBtn = UIHelper.createSecondaryButton("Copiar");
    private final String warningText;
    private final ModernFormDialog dialog;

    ApiKeySecretDialog(String secret) {
        this(secret, WARNING, "Chave copiada para a área de transferência.", "Chave de API criada", "fas-key",
                "Copie a chave e configure-a no sistema externo");
    }

    /** The webhook's signing secret, shown once after the {@code 201} (§4.4.2). */
    static ApiKeySecretDialog forWebhook(String secret) {
        return new ApiKeySecretDialog(secret, WEBHOOK_WARNING, "Segredo copiado para a área de transferência.",
                "Webhook criado", "fas-satellite-dish", "Copie o segredo e configure-o no sistema externo");
    }

    private ApiKeySecretDialog(String secret, String warningText, String copiedToast, String title, String icon,
                               String subtitle) {
        this.warningText = warningText;
        secretField.setText(secret);
        secretField.setEditable(false);
        UIHelper.styleTextField(secretField);

        JLabel warning = new JLabel(warningText);
        warning.setFont(Typography.CAPTION);
        warning.setForeground(UIHelper.PENDING_YELLOW);
        warning.setIcon(UIHelper.icon("fas-exclamation-triangle", 14, UIHelper.PENDING_YELLOW));

        copyBtn.setIcon(UIHelper.icon("fas-copy", 14));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(secretField.getText()), null);
            ToastManager.show(secretField, FeedbackType.SUCCESS, copiedToast);
        });

        JPanel content = new JPanel(new BorderLayout(0, Spacing.SM));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(Spacing.SM, 0, Spacing.SM, 0));
        content.add(secretField, BorderLayout.CENTER);
        content.add(warning, BorderLayout.SOUTH);

        dialog = new ModernFormDialog(UIHelper.mainWindow, title, icon, subtitle, content)
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
    String warningText() { return warningText; }
    ModernButton copyButton() { return copyBtn; }
}

package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;

/** Dialogo tematico para mensagens importantes e confirmacoes explicitas. */
public final class ModernMessageDialog {

    private ModernMessageDialog() {}

    public static void show(Window owner, FeedbackType type, String title, String message) {
        create(owner, type, title, message, false).showDialog();
    }

    public static boolean confirm(Window owner, FeedbackType type, String title, String message,
                                  String confirmLabel) {
        return create(owner, type, title, message, true)
                .setConfirmLabel(confirmLabel == null ? "Confirmar" : confirmLabel)
                .showDialog();
    }

    /** Prompt temático para substituir os inputs textuais legados do JOptionPane. */
    public static String prompt(Component owner, FeedbackType type, String title, String message,
                                String initialValue, boolean secret) {
        JTextField field = secret ? new JPasswordField() : new JTextField();
        if (initialValue != null) field.setText(initialValue);
        UIHelper.styleTextField(field);
        JPanel form = UIHelper.createDialogForm(message == null ? "Valor:" : message, field);
        String[] result = {null};
        ModernFormDialog dialog = new ModernFormDialog(resolveOwner(owner), title,
                (type == null ? FeedbackType.INFO : type).iconCode(), null, form)
                .setConfirmButton("Confirmar", "fas-check");
        dialog.setOnSave(() -> result[0] = field instanceof JPasswordField password
                ? new String(password.getPassword())
                : field.getText());
        return dialog.showDialog() ? result[0] : null;
    }

    private static Window resolveOwner(Component owner) {
        return owner instanceof Window window ? window : SwingUtilities.getWindowAncestor(owner);
    }

    static JPanel buildMessageBody(FeedbackType type, String message) {
        JPanel body = new ModernPanel(UIHelper.RADIUS_MD);
        body.setLayout(new BorderLayout(12, 0));
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel icon = new JLabel(UIHelper.icon(type.iconCode(), 24, type.color()));
        JLabel detail = new JLabel("<html><body style='width:400px'>" + escape(message) + "</body></html>");
        detail.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        detail.setForeground(UIHelper.TEXT_LIGHT);
        body.add(icon, BorderLayout.WEST);
        body.add(detail, BorderLayout.CENTER);
        body.getAccessibleContext().setAccessibleName(type.title() + ": " + message);
        return body;
    }

    private static Instance create(Window owner, FeedbackType type, String title, String message,
                                   boolean confirmation) {
        return new Instance(owner, type == null ? FeedbackType.INFO : type,
                title == null || title.isBlank() ? "Mensagem" : title,
                message == null ? "" : message, confirmation);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private static final class Instance {
        private final JDialog dialog;
        private final ModernButton confirmButton;
        private boolean confirmed;

        private Instance(Window owner, FeedbackType type, String title, String message, boolean confirmation) {
            dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.getContentPane().setBackground(UIHelper.BG_DARK);
            dialog.setIconImage(UIHelper.iconImage(type.iconCode(), 24, type.color()));

            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setBackground(UIHelper.BG_DARK);
            root.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
            root.add(UIHelper.buildPremiumHeader(type.iconCode(), title, null), BorderLayout.NORTH);
            root.add(buildMessageBody(type, message), BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            ModernButton cancel = UIHelper.createSecondaryButton(confirmation ? "Cancelar" : "Fechar");
            cancel.setIcon(UIHelper.icon("fas-times", 14));
            cancel.addActionListener(e -> dialog.dispose());
            actions.add(cancel);

            confirmButton = confirmation
                    ? (type == FeedbackType.ERROR
                        ? UIHelper.createDangerButton("Confirmar")
                        : UIHelper.createPrimaryButton("Confirmar"))
                    : null;
            if (confirmButton != null) {
                confirmButton.setIcon(UIHelper.icon("fas-check", 14));
                confirmButton.addActionListener(e -> {
                    confirmed = true;
                    dialog.dispose();
                });
                actions.add(confirmButton);
                dialog.getRootPane().setDefaultButton(confirmButton);
            }
            root.add(actions, BorderLayout.SOUTH);
            dialog.add(root);
            dialog.pack();
            dialog.setSize(Math.max(520, dialog.getWidth()), Math.max(260, dialog.getHeight()));
            dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            UIHelper.containWithinMain(dialog);
        }

        private Instance setConfirmLabel(String label) {
            if (confirmButton != null) confirmButton.setText(label);
            return this;
        }

        private boolean showDialog() {
            dialog.setVisible(true);
            return confirmed;
        }
    }
}

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

    public static void show(Component owner, FeedbackType type, String title, String message) {
        create(owner, type, title, message).showDialog();
    }

    public static boolean confirm(Component owner, FeedbackType type, String title, String message,
                                  String confirmLabel) {
        return create(owner, type, title, message, confirmLabel == null ? "Confirmar" : confirmLabel)
                .showDialog() == 0;
    }

    /**
     * Asks the operator to pick one of several actions. The last option is the main (filled) button,
     * the others are outlined; "Cancelar" always closes without a choice.
     *
     * @return the index of the chosen option, or {@code -1} when cancelled
     */
    public static int choose(Component owner, FeedbackType type, String title, String message,
                             String... options) {
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("choose() needs at least one option");
        }
        return create(owner, type, title, message, options).showDialog();
    }

    /** Shows a custom read-only body (a table, a log) in the themed frame with a single "Fechar". */
    public static void showContent(Component owner, String title, String iconCode, JComponent content) {
        new ModernFormDialog(resolveOwner(owner), title, iconCode, content).asReadOnly("Fechar").showDialog();
    }

    /** Shows a custom form in the themed frame; true when the operator confirms. */
    public static boolean confirmContent(Component owner, String title, String iconCode, JComponent content,
                                         String confirmLabel) {
        return new ModernFormDialog(resolveOwner(owner), title, iconCode, content)
                .setConfirmButton(confirmLabel == null ? "Confirmar" : confirmLabel, "fas-check")
                .showDialog();
    }

    /**
     * Last-resort message for the global error handler: tries the themed dialog and falls back to the
     * platform dialog only when the UI itself is broken (theme not installed, painter failure).
     */
    public static void showSafely(FeedbackType type, String title, String message) {
        try {
            show(UIHelper.mainWindow, type, title, message);
        } catch (RuntimeException | LinkageError themedDialogFailed) {
            javax.swing.JOptionPane.showMessageDialog(null, message, title,
                    type == FeedbackType.ERROR ? javax.swing.JOptionPane.ERROR_MESSAGE
                            : javax.swing.JOptionPane.WARNING_MESSAGE);
        }
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
        detail.setFont(Typography.EMPHASIS);
        detail.setForeground(UIHelper.TEXT_LIGHT);
        body.add(icon, BorderLayout.WEST);
        body.add(detail, BorderLayout.CENTER);
        body.getAccessibleContext().setAccessibleName(type.title() + ": " + message);
        return body;
    }

    private static Instance create(Component owner, FeedbackType type, String title, String message,
                                   String... choices) {
        return new Instance(owner == null ? null : resolveOwner(owner), type == null ? FeedbackType.INFO : type,
                title == null || title.isBlank() ? "Mensagem" : title,
                message == null ? "" : message, choices);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private static final class Instance {
        private final JDialog dialog;
        private int chosen = -1;

        private Instance(Window owner, FeedbackType type, String title, String message, String[] choices) {
            boolean confirmation = choices.length > 0;
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
            ModernButton cancel = ButtonStyles.secondary(UIHelper.createSecondaryButton(confirmation ? "Cancelar" : "Fechar"));
            cancel.setIcon(UIHelper.icon("fas-times", 14));
            cancel.addActionListener(e -> dialog.dispose());
            actions.add(cancel);

            for (int i = 0; i < choices.length; i++) {
                boolean main = i == choices.length - 1;
                ModernButton choice = !main
                        ? ButtonStyles.secondary(UIHelper.createSecondaryButton(choices[i]))
                        : (type == FeedbackType.ERROR
                            // Confirming a destructive action keeps the solid red warning on purpose.
                            ? UIHelper.createDangerButton(choices[i])
                            : ButtonStyles.main(UIHelper.createPrimaryButton(choices[i])));
                if (main) choice.setIcon(UIHelper.icon("fas-check", 14));
                int index = i;
                choice.addActionListener(e -> {
                    chosen = index;
                    dialog.dispose();
                });
                actions.add(choice);
                // Enter confirms, except a destructive action: there Enter is the safe "Cancelar".
                if (main) dialog.getRootPane().setDefaultButton(type == FeedbackType.ERROR ? cancel : choice);
            }
            root.add(actions, BorderLayout.SOUTH);
            dialog.add(root);
            dialog.pack();
            dialog.setSize(Math.max(520, dialog.getWidth()), Math.max(260, dialog.getHeight()));
            dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            UIHelper.containWithinMain(dialog);
        }

        private int showDialog() {
            dialog.setVisible(true);
            return chosen;
        }
    }
}

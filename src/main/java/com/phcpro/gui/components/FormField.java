package com.phcpro.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Font;

/** Campo canónico: label, obrigatório, conteúdo, ajuda e erro inline. */
public final class FormField extends JPanel {

    private final JComponent input;
    private final JLabel errorLabel = new JLabel(" ");
    private final boolean required;

    public FormField(String label, JComponent input, boolean required, String help) {
        super(new BorderLayout(0, 4));
        this.input = input;
        this.required = required;
        setOpaque(false);

        JLabel title = new JLabel(label + (required ? " *" : ""));
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        title.setForeground(UIHelper.ACCENT);
        title.setLabelFor(input);
        title.getAccessibleContext().setAccessibleName(label + (required ? ", obrigatório" : ""));
        add(title, BorderLayout.NORTH);
        add(input, BorderLayout.CENTER);

        JPanel messages = new JPanel(new BorderLayout());
        messages.setOpaque(false);
        if (help != null && !help.isBlank()) {
            JLabel helpLabel = new JLabel(help);
            helpLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            helpLabel.setForeground(UIHelper.TEXT_MUTED);
            messages.add(helpLabel, BorderLayout.NORTH);
        }
        errorLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        errorLabel.setForeground(UIHelper.REJECTED_RED);
        errorLabel.setVisible(false);
        messages.add(errorLabel, BorderLayout.SOUTH);
        add(messages, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    }

    public boolean validateRequired() {
        if (!required) {
            clearError();
            return true;
        }
        String value = input instanceof JTextComponent text ? text.getText() : null;
        if (value == null || value.isBlank()) {
            setError("Este campo é obrigatório.");
            return false;
        }
        clearError();
        return true;
    }

    public void setError(String message) {
        String safe = message == null || message.isBlank() ? "Valor inválido." : message;
        errorLabel.setText(safe);
        errorLabel.setVisible(true);
        UIHelper.markFieldInvalid(input, safe);
        revalidate();
    }

    public void clearError() {
        errorLabel.setText(" ");
        errorLabel.setVisible(false);
        UIHelper.clearFieldInvalid(input);
        revalidate();
    }

    public JComponent input() { return input; }
    public JLabel errorLabel() { return errorLabel; }
    public boolean required() { return required; }
}

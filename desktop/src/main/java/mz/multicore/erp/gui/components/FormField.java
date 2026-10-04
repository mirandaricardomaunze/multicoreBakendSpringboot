package mz.multicore.erp.gui.components;

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

    public boolean validateRegex(java.util.regex.Pattern pattern, String errorMessage) {
        if (!validateRequired()) return false;
        String value = input instanceof JTextComponent text ? text.getText().trim() : null;
        if (value == null || value.isEmpty()) return true;
        if (!pattern.matcher(value).matches()) {
            setError(errorMessage);
            return false;
        }
        clearError();
        return true;
    }

    public boolean validateEmail() {
        if (!validateRequired()) return false;
        String value = input instanceof JTextComponent text ? text.getText().trim() : null;
        if (value == null || value.isEmpty()) return true;
        if (!mz.multicore.erp.architecture.validation.ValidationPatterns.isValidEmail(value)) {
            setError("Formato de email inválido (ex.: utilizador@empresa.co.mz).");
            return false;
        }
        clearError();
        return true;
    }

    public boolean validateNuit() {
        if (!validateRequired()) return false;
        String value = input instanceof JTextComponent text ? text.getText().trim() : null;
        if (value == null || value.isEmpty()) return true;
        if (!mz.multicore.erp.architecture.validation.ValidationPatterns.isValidNuit(value)) {
            setError("NUIT inválido (deve conter 9 dígitos válidos segundo a AT).");
            return false;
        }
        clearError();
        return true;
    }

    public boolean validatePhone() {
        if (!validateRequired()) return false;
        String value = input instanceof JTextComponent text ? text.getText().trim() : null;
        if (value == null || value.isEmpty()) return true;
        if (!mz.multicore.erp.architecture.validation.ValidationPatterns.isValidPhone(value)) {
            setError("Telefone inválido (ex.: 841234567 ou +258 84 123 4567).");
            return false;
        }
        clearError();
        return true;
    }

    public boolean validateMinLength(int minLength, String fieldName) {
        if (!validateRequired()) return false;
        String value = input instanceof JTextComponent text ? text.getText().trim() : null;
        if (value == null || value.isEmpty()) return true;
        if (value.length() < minLength) {
            setError(fieldName + " deve ter no mínimo " + minLength + " caracteres.");
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

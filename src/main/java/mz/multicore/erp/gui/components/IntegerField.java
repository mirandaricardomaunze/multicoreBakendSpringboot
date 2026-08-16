package mz.multicore.erp.gui.components;

import javax.swing.JTextField;

/**
 * Input canónico de número inteiro (dias de prazo, tamanhos de página, contagens).
 * Mesmo contrato do {@link QuantityField}: {@code value()} valida, assinala o erro inline e
 * lança {@link IllegalArgumentException} com a mensagem a mostrar ao utilizador.
 */
public class IntegerField extends JTextField {

    private final int min;
    private final int max;
    private final String description;

    public IntegerField(String value, int min, int max, String description) {
        super(value == null ? "" : value);
        this.min = min;
        this.max = max;
        this.description = description;
        UIHelper.styleTextField(this);
        setHorizontalAlignment(RIGHT);
        getAccessibleContext().setAccessibleDescription(description);
    }

    /** Valor introduzido; campo vazio conta como {@code min} (o default do domínio). */
    public int value() {
        String text = getText() == null ? "" : getText().trim();
        if (text.isEmpty()) {
            UIHelper.clearFieldInvalid(this);
            return min;
        }
        try {
            int parsed = Integer.parseInt(text);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(description + " deve estar entre " + min + " e " + max + ".");
            }
            UIHelper.clearFieldInvalid(this);
            return parsed;
        } catch (NumberFormatException ex) {
            String message = "Introduza um número inteiro válido.";
            UIHelper.markFieldInvalid(this, message);
            throw new IllegalArgumentException(message);
        } catch (IllegalArgumentException ex) {
            UIHelper.markFieldInvalid(this, ex.getMessage());
            throw ex;
        }
    }
}

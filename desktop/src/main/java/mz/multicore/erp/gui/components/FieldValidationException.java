package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * A form value the operator must correct, tied to the field that holds it
 * (docs/VALIDACAO_CAMPOS_SPEC.md).
 *
 * <p>Thrown from a {@link ModernFormDialog} save handler, the dialog marks that field in red, writes the
 * message right under it and moves the cursor there; the mark goes away as soon as the field is edited.
 * It is still an {@link IllegalArgumentException}, so any other caller keeps showing the message as before.
 */
public final class FieldValidationException extends IllegalArgumentException {

    private final transient JComponent field;

    private FieldValidationException(JComponent field, String message) {
        super(message);
        this.field = Objects.requireNonNull(field, "field");
    }

    public static FieldValidationException of(JComponent field, String message) {
        return new FieldValidationException(field, message);
    }

    public JComponent field() {
        return field;
    }
}

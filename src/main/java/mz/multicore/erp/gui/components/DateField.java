package mz.multicore.erp.gui.components;

import javax.swing.JTextField;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Input canónico de data ISO usado pelos contratos HTTP (`yyyy-MM-dd`). */
public class DateField extends JTextField {

    public DateField() { this(null); }

    public DateField(LocalDate value) {
        super(value == null ? "" : value.toString());
        UIHelper.styleTextField(this);
        setToolTipText("Formato: yyyy-MM-dd");
        getAccessibleContext().setAccessibleDescription("Data no formato ano-mês-dia");
    }

    public LocalDate value() {
        try {
            LocalDate parsed = LocalDate.parse(getText().trim());
            UIHelper.clearFieldInvalid(this);
            return parsed;
        } catch (DateTimeParseException ex) {
            String message = "Introduza uma data válida no formato yyyy-MM-dd.";
            UIHelper.markFieldInvalid(this, message);
            throw new IllegalArgumentException(message);
        }
    }
}

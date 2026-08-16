package mz.multicore.erp.gui.components;

import javax.swing.JTextField;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Input canónico de quantidade com três casas decimais. */
public class QuantityField extends JTextField {

    private final boolean positiveRequired;

    public QuantityField(String value, boolean positiveRequired) {
        super(value == null ? "" : value);
        this.positiveRequired = positiveRequired;
        UIHelper.styleTextField(this);
        setHorizontalAlignment(RIGHT);
        getAccessibleContext().setAccessibleDescription("Quantidade");
    }

    public BigDecimal value() {
        try {
            BigDecimal parsed = new BigDecimal(MoneyField.normalize(getText())).setScale(3, RoundingMode.HALF_UP);
            if (positiveRequired && parsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
            }
            UIHelper.clearFieldInvalid(this);
            return parsed;
        } catch (NumberFormatException ex) {
            String message = "Introduza uma quantidade válida.";
            UIHelper.markFieldInvalid(this, message);
            throw new IllegalArgumentException(message);
        } catch (IllegalArgumentException ex) {
            UIHelper.markFieldInvalid(this, ex.getMessage());
            throw ex;
        }
    }
}

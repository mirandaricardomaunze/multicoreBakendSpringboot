package com.phcpro.gui.components;

import javax.swing.JTextField;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Número decimal genérico para taxas e percentagens que não são montantes monetários. */
public class DecimalField extends JTextField {

    private final int scale;
    private final boolean positiveRequired;

    public DecimalField(String value, int scale, boolean positiveRequired) {
        super(value == null ? "" : value);
        this.scale = scale;
        this.positiveRequired = positiveRequired;
        UIHelper.styleTextField(this);
        setHorizontalAlignment(RIGHT);
    }

    public BigDecimal value() {
        try {
            BigDecimal parsed = new BigDecimal(MoneyField.normalize(getText()))
                    .setScale(scale, RoundingMode.HALF_UP);
            if (positiveRequired && parsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("O valor deve ser maior que zero.");
            }
            UIHelper.clearFieldInvalid(this);
            return parsed;
        } catch (NumberFormatException ex) {
            String message = "Introduza um valor decimal válido.";
            UIHelper.markFieldInvalid(this, message);
            throw new IllegalArgumentException(message);
        } catch (IllegalArgumentException ex) {
            UIHelper.markFieldInvalid(this, ex.getMessage());
            throw ex;
        }
    }
}

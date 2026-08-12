package com.phcpro.gui.components;

import javax.swing.JTextField;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Input monetário canónico; aceita vírgula ou ponto decimal. */
public class MoneyField extends JTextField {

    public MoneyField() { this(""); }

    public MoneyField(String value) {
        super(value == null ? "" : value);
        UIHelper.styleTextField(this);
        setHorizontalAlignment(RIGHT);
        getAccessibleContext().setAccessibleDescription("Valor monetário");
    }

    public BigDecimal value() {
        try {
            BigDecimal parsed = new BigDecimal(normalize(getText())).setScale(2, RoundingMode.HALF_UP);
            UIHelper.clearFieldInvalid(this);
            return parsed;
        } catch (RuntimeException ex) {
            String message = "Introduza um valor monetário válido.";
            UIHelper.markFieldInvalid(this, message);
            throw new IllegalArgumentException(message);
        }
    }

    static String normalize(String text) {
        String value = text == null ? "" : text.trim().replace("\u00A0", "").replace(" ", "");
        if (value.isEmpty()) throw new NumberFormatException("empty");
        int comma = value.lastIndexOf(',');
        int dot = value.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            char decimal = comma > dot ? ',' : '.';
            char grouping = decimal == ',' ? '.' : ',';
            value = value.replace(String.valueOf(grouping), "");
            if (decimal == ',') value = value.replace(',', '.');
        } else if (comma >= 0) {
            value = value.replace(',', '.');
        }
        return value;
    }
}

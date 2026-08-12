package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalFormFieldsTest {

    @Test
    void moneyField_espacoEMilharComVirgula_converteBigDecimal() {
        MoneyField field = new MoneyField("1 234,50");
        assertEquals(new BigDecimal("1234.50"), field.value());
    }

    @Test
    void moneyField_textoInvalido_marcaErroAcessivel() {
        MoneyField field = new MoneyField("abc");
        assertThrows(IllegalArgumentException.class, field::value);
        assertEquals("Introduza um valor monetário válido.",
                field.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void quantityField_virgula_converteComTresCasas() {
        QuantityField field = new QuantityField("1,250", true);
        assertEquals(new BigDecimal("1.250"), field.value());
    }

    @Test
    void quantityField_zeroQuandoPositiva_recusa() {
        QuantityField field = new QuantityField("0", true);
        assertThrows(IllegalArgumentException.class, field::value);
        assertEquals("A quantidade deve ser maior que zero.",
                field.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void dateField_iso_converteLocalDate() {
        DateField field = new DateField();
        field.setText("2026-08-09");
        assertEquals(LocalDate.of(2026, 8, 9), field.value());
    }

    @Test
    void dateField_invalida_marcaErroAcessivel() {
        DateField field = new DateField();
        field.setText("09/08/2026");
        assertThrows(IllegalArgumentException.class, field::value);
        assertTrue(field.getAccessibleContext().getAccessibleDescription().contains("yyyy-MM-dd"));
    }

    @Test
    void decimalField_taxaPreservaQuatroCasas() {
        DecimalField field = new DecimalField("0,1655", 4, false);
        assertEquals(new BigDecimal("0.1655"), field.value());
    }

    @Test
    void formField_obrigatorioVazio_mostraErroInline() {
        JTextField input = new JTextField();
        UIHelper.styleTextField(input);
        FormField field = new FormField("Nome", input, true, null);

        assertFalse(field.validateRequired());
        assertTrue(field.errorLabel().isVisible());
        assertEquals("Este campo é obrigatório.", input.getClientProperty("validationError"));
    }

    @Test
    void readOnly_aplicaEstadoSemantico() {
        JTextField input = new JTextField("FT 1/2026");
        UIHelper.styleTextField(input);
        UIHelper.setReadOnly(input, true);

        assertFalse(input.isEditable());
        assertFalse(input.isFocusable());
        assertEquals(Boolean.TRUE, input.getClientProperty("readOnly"));
    }
}

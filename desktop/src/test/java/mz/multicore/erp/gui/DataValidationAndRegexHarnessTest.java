package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.validation.ValidationPatterns;
import mz.multicore.erp.gui.components.FormField;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de teste para validação de dados e expressões regulares (Frontend & Backend).
 */
class DataValidationAndRegexHarnessTest {

    @Test
    @DisplayName("DVR-01: Validação de Email canónica (RFC 5322 simplificada)")
    void testEmailValidation() {
        assertTrue(ValidationPatterns.isValidEmail("contacto@empresa.co.mz"));
        assertTrue(ValidationPatterns.isValidEmail("admin.erp@multicore.co.mz"));
        assertTrue(ValidationPatterns.isValidEmail("user+tag@domain.com"));

        assertFalse(ValidationPatterns.isValidEmail(null));
        assertFalse(ValidationPatterns.isValidEmail(""));
        assertFalse(ValidationPatterns.isValidEmail("   "));
        assertFalse(ValidationPatterns.isValidEmail("invalido@"));
        assertFalse(ValidationPatterns.isValidEmail("@dominio.com"));
        assertFalse(ValidationPatterns.isValidEmail("sem-arroba.com"));
        assertFalse(ValidationPatterns.isValidEmail("user@dominio.c")); // TLD com 1 letra
    }

    @Test
    @DisplayName("DVR-02: Validação de NUIT moçambicano (Módulo 11 e Consumidor Final)")
    void testNuitValidation() {
        // Consumidor final canónico
        assertTrue(ValidationPatterns.isValidNuit("999999999"));
        assertTrue(ValidationPatterns.isValidNuit(" 999999999 "));

        // NUIT gerado com algoritmo Módulo 11 oficial
        String base = "40012345";
        int[] weights = {9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.digit(base.charAt(i), 10) * weights[i];
        }
        int remainder = sum % 11;
        int checkDigit = remainder < 2 ? 0 : 11 - remainder;
        String validNuit = base + checkDigit;

        assertTrue(ValidationPatterns.isValidNuit(validNuit));

        // NUIT inválido (comprimento, caracteres ou dígito errado)
        assertFalse(ValidationPatterns.isValidNuit(null));
        assertFalse(ValidationPatterns.isValidNuit(""));
        assertFalse(ValidationPatterns.isValidNuit("12345678")); // 8 dígitos
        assertFalse(ValidationPatterns.isValidNuit("1234567890")); // 10 dígitos
        assertFalse(ValidationPatterns.isValidNuit("12345678A")); // com letra
        assertFalse(ValidationPatterns.isValidNuit(base + ((checkDigit + 1) % 10))); // dígito de controlo adulterado
    }

    @Test
    @DisplayName("DVR-03: Validação de Telefones de Moçambique (Vodacom, Tmcel, Movitel e Fixos)")
    void testPhoneValidation() {
        // Móveis
        assertTrue(ValidationPatterns.isValidPhone("841234567")); // Vodacom
        assertTrue(ValidationPatterns.isValidPhone("851234567")); // Vodacom
        assertTrue(ValidationPatterns.isValidPhone("821234567")); // Tmcel
        assertTrue(ValidationPatterns.isValidPhone("831234567")); // Tmcel
        assertTrue(ValidationPatterns.isValidPhone("861234567")); // Movitel
        assertTrue(ValidationPatterns.isValidPhone("871234567")); // Movitel

        // Com código do país +258
        assertTrue(ValidationPatterns.isValidPhone("+258841234567"));
        assertTrue(ValidationPatterns.isValidPhone("+258 84 123 4567"));
        assertTrue(ValidationPatterns.isValidPhone("258841234567"));

        // Redes fixas
        assertTrue(ValidationPatterns.isValidPhone("21123456")); // Maputo fixo
        assertTrue(ValidationPatterns.isValidPhone("+258 21 123456"));

        // Telefones inválidos
        assertFalse(ValidationPatterns.isValidPhone(null));
        assertFalse(ValidationPatterns.isValidPhone(""));
        assertFalse(ValidationPatterns.isValidPhone("811234567")); // prefixo inexistente
        assertFalse(ValidationPatterns.isValidPhone("12345"));
        assertFalse(ValidationPatterns.isValidPhone("8412345678")); // dígitos a mais

        // Teste do utilitário de limpeza
        assertEquals("841234567", ValidationPatterns.cleanPhoneMozambique("+258 84 123 4567"));
        assertEquals("841234567", ValidationPatterns.cleanPhoneMozambique("841234567"));
    }

    @Test
    @DisplayName("DVR-04: Validação de B.I., Barcode, SKU e Valores Monetários")
    void testOtherEntityValidations() {
        // B.I. Moçambicano (12 dígitos + 1 letra maiúscula)
        assertTrue(ValidationPatterns.isValidBi("110100234567B"));
        assertTrue(ValidationPatterns.isValidBi("010100123456z")); // case insensitive check
        assertFalse(ValidationPatterns.isValidBi("1101002345678")); // sem letra
        assertFalse(ValidationPatterns.isValidBi("11010023456B")); // 11 dígitos
        assertFalse(ValidationPatterns.isValidBi(null));

        // Código de barras (4 a 30 caracteres válidos)
        assertTrue(ValidationPatterns.isValidBarcode("6001234567890"));
        assertTrue(ValidationPatterns.isValidBarcode("PROD-123_A"));
        assertFalse(ValidationPatterns.isValidBarcode("12")); // muito curto
        assertFalse(ValidationPatterns.isValidBarcode("PROD 123")); // espaço ilegal

        // SKU (2 a 30 caracteres válidos)
        assertTrue(ValidationPatterns.isValidSku("ART-01"));
        assertTrue(ValidationPatterns.isValidSku("SKU12345"));
        assertFalse(ValidationPatterns.isValidSku("A")); // muito curto
        assertFalse(ValidationPatterns.isValidSku(null));

        // Valores numéricos
        assertTrue(ValidationPatterns.isValidPositiveAmount(BigDecimal.valueOf(100.50)));
        assertFalse(ValidationPatterns.isValidPositiveAmount(BigDecimal.ZERO));
        assertFalse(ValidationPatterns.isValidPositiveAmount(BigDecimal.valueOf(-10)));
        assertFalse(ValidationPatterns.isValidPositiveAmount(null));

        assertTrue(ValidationPatterns.isValidPercentage(BigDecimal.valueOf(16)));
        assertTrue(ValidationPatterns.isValidPercentage(BigDecimal.ZERO));
        assertTrue(ValidationPatterns.isValidPercentage(BigDecimal.valueOf(100)));
        assertFalse(ValidationPatterns.isValidPercentage(BigDecimal.valueOf(100.1)));
        assertFalse(ValidationPatterns.isValidPercentage(BigDecimal.valueOf(-1)));
    }

    @Test
    @DisplayName("DVR-05: FormField integra validações inline e mantém foco")
    void testFormFieldValidation() {
        JTextField emailField = new JTextField("invalido@");
        FormField emailForm = new FormField("Email", emailField, true, null);
        assertFalse(emailForm.validateEmail(), "Email com formato errado deve falhar");
        assertTrue(emailForm.errorLabel().isVisible(), "Erro deve ser exibido na interface");
        assertTrue(emailForm.errorLabel().getText().contains("Email inválido") || emailForm.errorLabel().getText().contains("Formato de email"));

        emailField.setText("suporte@multicore.co.mz");
        assertTrue(emailForm.validateEmail(), "Email corrigido deve ser válido");
        assertFalse(emailForm.errorLabel().isVisible(), "Erro deve ser limpo");

        JTextField phoneField = new JTextField("99999");
        FormField phoneForm = new FormField("Telefone", phoneField, false, null);
        assertFalse(phoneForm.validatePhone(), "Telefone com formato errado deve falhar");

        phoneField.setText("841234567");
        assertTrue(phoneForm.validatePhone(), "Telefone válido deve ser aceite");

        JTextField nameField = new JTextField("AB");
        FormField nameForm = new FormField("Nome", nameField, true, null);
        assertFalse(nameForm.validateMinLength(3, "Nome"), "Nome com menos de 3 caracteres deve falhar");

        nameField.setText("Multicore, Lda.");
        assertTrue(nameForm.validateMinLength(3, "Nome"), "Nome válido deve ser aceite");
    }

    @Test
    @DisplayName("DVR-06: UIHelper filtros reactivos de documento (digitação em tempo real)")
    void testUiHelperDocumentFilters() throws BadLocationException {
        // Teste do filtro de apenas dígitos com limite de caracteres
        JTextField digitsField = new JTextField();
        UIHelper.installDigitsOnlyFilter(digitsField, 9);

        digitsField.getDocument().insertString(0, "12a3b4c567890extra", null);
        assertEquals("123456789", digitsField.getText(), "Deve aceitar apenas algarismos e truncar em 9 dígitos");

        // Teste do filtro de conversão para maiúsculas
        JTextField upperField = new JTextField();
        UIHelper.installUppercaseFilter(upperField, 13);

        upperField.getDocument().insertString(0, "110100234567b", null);
        assertEquals("110100234567B", upperField.getText(), "Deve converter letras para maiúsculas automaticamente");
    }
}

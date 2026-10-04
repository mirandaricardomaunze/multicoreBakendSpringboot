package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.CustomerCreditValidator;
import mz.multicore.erp.gui.components.NuitValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de teste para validação de NUIT moçambicano e limites de crédito.
 */
class NuitAndCreditLimitValidationHarnessTest {

    @Test
    @DisplayName("NCL-01: NuitValidator valida regras da Autoridade Tributária")
    void testNuitValidatorRules() {
        // Consumidor final é válido
        assertTrue(NuitValidator.isValid("999999999"), "999999999 deve ser válido");

        // Dígito nulo ou comprimento errado
        assertFalse(NuitValidator.isValid(null));
        assertFalse(NuitValidator.isValid(""));
        assertFalse(NuitValidator.isValid("12345678")); // 8 dígitos
        assertFalse(NuitValidator.isValid("1234567890")); // 10 dígitos
        assertFalse(NuitValidator.isValid("12345678A")); // com letras

        // Dígitos todos iguais (rejeitado)
        assertFalse(NuitValidator.isValid("111111111"));
        assertFalse(NuitValidator.isValid("000000000"));

        // NUIT gerado com algoritmo Módulo 11 oficial
        String base = "40012345";
        int check = NuitValidator.calculateCheckDigit(base);
        String validNuit = base + check;
        assertTrue(NuitValidator.isValid(validNuit), "NUIT gerado com check digit deve ser válido");

        // NUIT com dígito de controlo alterado
        int wrongCheck = (check + 1) % 10;
        String invalidNuit = base + wrongCheck;
        assertFalse(NuitValidator.isValid(invalidNuit), "NUIT com dígito errado deve ser rejeitado");
    }

    @Test
    @DisplayName("NCL-02: CustomerCreditValidator bloqueia mora e excesso de limite")
    void testCustomerCreditValidator() {
        BigDecimal limit = BigDecimal.valueOf(50000);
        BigDecimal debt = BigDecimal.valueOf(30000);

        // Caso 1: Venda normal dentro do saldo disponível (20.000 disponível, pede 15.000)
        var assessNormal = CustomerCreditValidator.evaluate(limit, debt, BigDecimal.valueOf(15000), false);
        assertTrue(assessNormal.isApproved(), "Deve aprovar crédito dentro do limite");
        assertEquals(CustomerCreditValidator.CreditCheckStatus.APPROVED, assessNormal.status());

        // Caso 2: Venda excede o limite disponível (20.000 disponível, pede 25.000)
        var assessExceeded = CustomerCreditValidator.evaluate(limit, debt, BigDecimal.valueOf(25000), false);
        assertFalse(assessExceeded.isApproved(), "Deve recusar crédito quando excede o limite");
        assertEquals(CustomerCreditValidator.CreditCheckStatus.BLOCKED_LIMIT_EXCEEDED, assessExceeded.status());

        // Caso 3: Cliente tem faturas em atraso (hasOverdue = true)
        var assessOverdue = CustomerCreditValidator.evaluate(limit, debt, BigDecimal.valueOf(5000), true);
        assertFalse(assessOverdue.isApproved(), "Deve recusar crédito quando há faturas em mora");
        assertEquals(CustomerCreditValidator.CreditCheckStatus.BLOCKED_OVERDUE, assessOverdue.status());

        // Caso 4: Cliente pronto pagamento (limite = 0)
        var assessZero = CustomerCreditValidator.evaluate(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100), false);
        assertFalse(assessZero.isApproved(), "Deve recusar crédito para cliente pronto pagamento");
        assertEquals(CustomerCreditValidator.CreditCheckStatus.NO_CREDIT_ALLOWED, assessZero.status());

        // Caso 5: Crédito flexível (limite null)
        var assessFlexible = CustomerCreditValidator.evaluate(null, debt, BigDecimal.valueOf(100000), false);
        assertTrue(assessFlexible.isApproved(), "Crédito sem limite configurado deve ser aprovado");
    }
}

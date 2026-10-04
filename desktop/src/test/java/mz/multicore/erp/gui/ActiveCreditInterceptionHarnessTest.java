package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.CustomerCreditValidator;
import mz.multicore.erp.gui.components.CustomerCreditValidator.CreditAssessment;
import mz.multicore.erp.gui.components.CustomerCreditValidator.CreditCheckStatus;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de validação da intercepção activa de crédito na faturação e POS.
 */
class ActiveCreditInterceptionHarnessTest {

    private static Path resolveSourcePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", "desktop", relativePath);
    }

    private ClientDTO createClient(Long id, String name, BigDecimal creditLimit) {
        return new ClientDTO(id, name, "123456789", "cliente@empresa.co.mz",
                "Av. 25 de Setembro, Maputo", 30, creditLimit);
    }

    @Test
    @DisplayName("ACI-01: validateCreditPreFlight rejeita vendas a crédito para cliente avulso ou com limite 0,00 MT")
    void testValidateCreditPreFlightRules() {
        // 1. Cliente nulo (avulso)
        CreditAssessment nullAssessment = CustomerCreditValidator.validateCreditPreFlight(null, new BigDecimal("500.00"));
        assertFalse(nullAssessment.isApproved(), "Venda a crédito sem cliente deve ser rejeitada");
        assertEquals(CreditCheckStatus.NO_CREDIT_ALLOWED, nullAssessment.status());
        assertTrue(nullAssessment.details().contains("cliente cadastrado"));

        // 2. Cliente com limite zero (pronto pagamento exclusivo)
        ClientDTO zeroLimitClient = createClient(1L, "Supermercado Popular", BigDecimal.ZERO);
        CreditAssessment zeroAssessment = CustomerCreditValidator.validateCreditPreFlight(zeroLimitClient, new BigDecimal("1200.00"));
        assertFalse(zeroAssessment.isApproved(), "Cliente com limite 0 deve ser bloqueado");
        assertEquals(CreditCheckStatus.NO_CREDIT_ALLOWED, zeroAssessment.status());
        assertTrue(zeroAssessment.details().contains("não tem autorização para vendas a crédito"));

        // 3. Cliente com crédito flexível (limite null)
        ClientDTO flexibleClient = createClient(2L, "Construtora Horizonte", null);
        CreditAssessment flexAssessment = CustomerCreditValidator.validateCreditPreFlight(flexibleClient, new BigDecimal("50000.00"));
        assertTrue(flexAssessment.isApproved(), "Cliente com limite flexível deve ser aprovado");
        assertEquals(CreditCheckStatus.APPROVED, flexAssessment.status());

        // 4. Cliente com limite positivo suficiente
        ClientDTO goodCreditClient = createClient(3L, "Farmácia Central", new BigDecimal("25000.00"));
        CreditAssessment approvedAssessment = CustomerCreditValidator.validateCreditPreFlight(goodCreditClient, new BigDecimal("8000.00"));
        assertTrue(approvedAssessment.isApproved(), "Venda dentro do limite deve ser aprovada");
        assertEquals(CreditCheckStatus.APPROVED, approvedAssessment.status());

        // 5. Cliente com limite excedido
        CreditAssessment exceededAssessment = CustomerCreditValidator.validateCreditPreFlight(goodCreditClient, new BigDecimal("30000.00"));
        assertFalse(exceededAssessment.isApproved(), "Venda acima do limite deve ser rejeitada");
        assertEquals(CreditCheckStatus.BLOCKED_LIMIT_EXCEEDED, exceededAssessment.status());
    }

    @Test
    @DisplayName("ACI-02: POSPanel e ComercialPanel integram pre-flight credit check sem emojis")
    void testPanelIntegrationAndConventions() throws IOException {
        Path posPath = resolveSourcePath("src/main/java/mz/multicore/erp/gui/POSPanel.java");
        assertTrue(Files.exists(posPath), "POSPanel.java deve existir");
        String posContent = Files.readString(posPath);

        assertTrue(posContent.contains("CustomerCreditValidator.validateCreditPreFlight"),
                "POSPanel deve chamar CustomerCreditValidator.validateCreditPreFlight");
        assertFalse(posContent.contains("⭐") || posContent.contains("✅") || posContent.contains("❌"), "Sem emojis");

        Path comercialPath = resolveSourcePath("src/main/java/mz/multicore/erp/gui/ComercialPanel.java");
        assertTrue(Files.exists(comercialPath), "ComercialPanel.java deve existir");
        String comercialContent = Files.readString(comercialPath);

        assertTrue(comercialContent.contains("CustomerCreditValidator.validateCreditPreFlight"),
                "ComercialPanel deve chamar CustomerCreditValidator.validateCreditPreFlight");
        assertFalse(comercialContent.contains("⭐") || comercialContent.contains("✅") || comercialContent.contains("❌"), "Sem emojis");
    }
}

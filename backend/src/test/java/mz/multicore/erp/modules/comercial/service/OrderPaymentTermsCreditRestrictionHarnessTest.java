package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.ClientCreditEligibilityDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPaymentTermsCreditRestrictionHarnessTest {

    private InvoiceRepository invoiceRepository;
    private ComercialService comercialService;

    private static final Long COMPANY_ID = 1L;
    private static final Long CLIENT_ID = 100L;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);

        comercialService = new ComercialService(
                mock(ClientRepository.class),
                mock(ProductRepository.class),
                mock(ProductCategoryRepository.class),
                invoiceRepository,
                mock(ApprovalService.class),
                mock(CompanyRepository.class),
                mock(WarehouseRepository.class),
                mock(InventoryService.class),
                mock(ReceiptRepository.class),
                mock(FinanceService.class),
                mock(TreasuryAccountRepository.class),
                mock(OrderRepository.class),
                mock(OrderLineRepository.class),
                mock(WalkInClientProvider.class),
                mock(DocumentNumberService.class),
                mock(AuditLogService.class),
                mock(TaxRateRepository.class),
                mock(ReceivablesService.class),
                mock(ApplicationEventPublisher.class)
        );

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("operador", "USER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("Consumidor Final (sem cliente registado) apenas aceita Pronto Pagamento")
    void consumidorFinal_apenasProntoPagamento() {
        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(null, null, COMPANY_ID));
        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(null, "Pronto Pagamento", COMPANY_ID));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                comercialService.validatePaymentTermsForClient(null, "30 dias", COMPANY_ID));
        assertTrue(ex.getMessage().contains("Consumidor Final"));
    }

    @Test
    @DisplayName("Cliente com limite de crédito 0 é problemático: crédito proibido, pronto pagamento permitido")
    void clienteComLimiteZero_proibeCredito() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setName("Cliente Sem Credito");
        client.setCreditLimit(BigDecimal.ZERO);
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        ClientCreditEligibilityDTO eligibility = comercialService.evaluateClientCreditEligibility(client, COMPANY_ID);
        assertTrue(eligibility.problematic());
        assertFalse(eligibility.creditAllowed());
        assertEquals(0, eligibility.defaultPaymentTermsDays());

        // Deve falhar com 15 dias
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                comercialService.validatePaymentTermsForClient(client, "15 dias", COMPANY_ID));
        assertTrue(ex.getMessage().toLowerCase().contains("restri") || ex.getMessage().contains("Pronto Pagamento"));

        // Deve passar com Pronto Pagamento
        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(client, "Pronto Pagamento", COMPANY_ID));
    }

    @Test
    @DisplayName("Cliente com faturas vencidas há mais de 30 dias é problemático e restrito a pronto pagamento")
    void clienteComMora_proibeCredito() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setName("Cliente Inadimplente");
        client.setCreditLimit(new BigDecimal("100000.00"));

        Invoice overdue = new Invoice();
        overdue.setId(99L);
        overdue.setClient(client);
        overdue.setStatus(InvoiceStatus.APPROVED);
        overdue.setTotalAmount(new BigDecimal("10000.00"));
        overdue.setAmountPaid(BigDecimal.ZERO);
        overdue.setDueDate(LocalDate.now().minusDays(35));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(overdue));

        ClientCreditEligibilityDTO eligibility = comercialService.evaluateClientCreditEligibility(client, COMPANY_ID);
        assertTrue(eligibility.problematic());
        assertFalse(eligibility.creditAllowed());
        assertTrue(eligibility.restrictionReason().contains("mora"));

        // Tentativa de 30 dias
        assertThrows(BusinessRuleException.class, () ->
                comercialService.validatePaymentTermsForClient(client, "30 dias", COMPANY_ID));

        // Pronto pagamento permitido
        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(client, "Pronto Pagamento", COMPANY_ID));
    }

    @Test
    @DisplayName("Cliente com limite de crédito esgotado é restrito a pronto pagamento")
    void clienteComLimiteEsgotado_proibeCredito() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setName("Cliente Limite Esgotado");
        client.setCreditLimit(new BigDecimal("20000.00"));

        Invoice pending = new Invoice();
        pending.setId(88L);
        pending.setClient(client);
        pending.setStatus(InvoiceStatus.APPROVED);
        pending.setTotalAmount(new BigDecimal("25000.00"));
        pending.setAmountPaid(BigDecimal.ZERO);
        pending.setDueDate(LocalDate.now().plusDays(5));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(pending));

        ClientCreditEligibilityDTO eligibility = comercialService.evaluateClientCreditEligibility(client, COMPANY_ID);
        assertTrue(eligibility.problematic());
        assertFalse(eligibility.creditAllowed());
        assertTrue(eligibility.restrictionReason().contains("esgotado"));

        assertThrows(BusinessRuleException.class, () ->
                comercialService.validatePaymentTermsForClient(client, "60 dias", COMPANY_ID));
        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(client, "Pronto Pagamento", COMPANY_ID));
    }

    @Test
    @DisplayName("Cliente saudável pode comprar a crédito e a pronto pagamento")
    void clienteSaudavel_permiteCredito() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setName("Cliente Bom Pagador");
        client.setCreditLimit(new BigDecimal("200000.00"));
        client.setPaymentTermsDays(30);
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        ClientCreditEligibilityDTO eligibility = comercialService.evaluateClientCreditEligibility(client, COMPANY_ID);
        assertFalse(eligibility.problematic());
        assertTrue(eligibility.creditAllowed());
        assertEquals(30, eligibility.defaultPaymentTermsDays());

        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(client, "30 dias", COMPANY_ID));
        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(client, "60 dias", COMPANY_ID));
        assertDoesNotThrow(() -> comercialService.validatePaymentTermsForClient(client, "Pronto Pagamento", COMPANY_ID));
    }

    @Test
    @DisplayName("Cálculo de vencimento a partir das condições de pagamento")
    void calculoDeVencimento() {
        LocalDate issueDate = LocalDate.of(2026, 10, 20);
        Client client = new Client();
        client.setPaymentTermsDays(15);

        assertEquals(issueDate, comercialService.resolveDueDateFromTerms("Pronto Pagamento", issueDate, client));
        assertEquals(issueDate.plusDays(15), comercialService.resolveDueDateFromTerms("", issueDate, client));
        assertEquals(issueDate.plusDays(15), comercialService.resolveDueDateFromTerms(null, issueDate, client));

        assertEquals(issueDate.plusDays(15), comercialService.resolveDueDateFromTerms("15 dias", issueDate, client));
        assertEquals(issueDate.plusDays(30), comercialService.resolveDueDateFromTerms("30 dias", issueDate, client));
        assertEquals(issueDate.plusDays(45), comercialService.resolveDueDateFromTerms("45 dias", issueDate, client));
        assertEquals(issueDate.plusDays(60), comercialService.resolveDueDateFromTerms("60 dias", issueDate, client));
        assertEquals(issueDate.plusDays(90), comercialService.resolveDueDateFromTerms("90 dias", issueDate, client));
    }
}

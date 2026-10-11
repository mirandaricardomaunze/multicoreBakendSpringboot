package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderRiskEvaluatorTest {

    private InvoiceRepository invoiceRepository;
    private OrderRiskEvaluator evaluator;

    private static final Long COMPANY_ID = 1L;
    private static final Long CLIENT_ID = 10L;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        evaluator = new OrderRiskEvaluator(invoiceRepository);
    }

    @Test
    @DisplayName("Green Lane: Encomenda de 15.000 MT sem mora, crédito saudável e desconto <= 10% é auto-aprovada")
    void greenLane_encomendaPequenaSemRisco_autoAprova() {
        Client client = client(CLIENT_ID, "Padaria Central", new BigDecimal("50000.00"));
        Order order = order("EC-001", client, new BigDecimal("15000.00"), new BigDecimal("5.00"));

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertTrue(assessment.autoApproved());
        assertEquals("NONE", assessment.requiredRole());
        assertTrue(assessment.triggers().isEmpty());
        assertTrue(assessment.description().contains("Green Lane"));
    }

    @Test
    @DisplayName("Mora Moderada (35 dias): Retém a encomenda e exige aprovação de MANAGER")
    void mora_35Dias_requerManager() {
        Client client = client(CLIENT_ID, "Comercial do Norte", new BigDecimal("100000.00"));
        Order order = order("EC-002", client, new BigDecimal("20000.00"), BigDecimal.ZERO);

        Invoice overdue = overdueInvoice(client, new BigDecimal("5000.00"), 35);
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(overdue));

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("MANAGER", assessment.requiredRole());
        assertTrue(assessment.description().contains("Risco de Mora"));
    }

    @Test
    @DisplayName("Inadimplência Crítica (>60 dias): Escala para aprovação de ADMIN")
    void mora_65Dias_requerAdmin() {
        Client client = client(CLIENT_ID, "Transportes Rápidos", new BigDecimal("100000.00"));
        Order order = order("EC-003", client, new BigDecimal("10000.00"), BigDecimal.ZERO);

        Invoice overdue = overdueInvoice(client, new BigDecimal("15000.00"), 65);
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(overdue));

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("ADMIN", assessment.requiredRole());
        assertTrue(assessment.description().contains("Inadimplência Crítica"));
    }

    @Test
    @DisplayName("Limite de Crédito Excedido até 20%: Exige perfil MANAGER")
    void limiteCredito_excedidoModerado_requerManager() {
        Client client = client(CLIENT_ID, "Construtora Alfa", new BigDecimal("100000.00"));
        // Limite 100.000, encomenda 115.000 -> Excesso de 15% (<= 20%)
        Order order = order("EC-004", client, new BigDecimal("115000.00"), BigDecimal.ZERO);

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("MANAGER", assessment.requiredRole());
        assertTrue(assessment.description().contains("Limite de Crédito Excedido"));
    }

    @Test
    @DisplayName("Limite de Crédito Excedido > 20%: Escala para ADMIN")
    void limiteCredito_excedidoCritico_requerAdmin() {
        Client client = client(CLIENT_ID, "Armazéns Sol", new BigDecimal("50000.00"));
        // Limite 50.000, encomenda 70.000 -> Excesso de 40% (> 20%)
        Order order = order("EC-005", client, new BigDecimal("70000.00"), BigDecimal.ZERO);

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("ADMIN", assessment.requiredRole());
        assertTrue(assessment.description().contains("Limite de Crédito Excedido"));
    }

    @Test
    @DisplayName("Desconto Comercial > 10% e <= 25%: Requer MANAGER")
    void desconto_comercial15Pct_requerManager() {
        Client client = client(CLIENT_ID, "Supermercado Viva", new BigDecimal("100000.00"));
        Order order = order("EC-006", client, new BigDecimal("30000.00"), new BigDecimal("15.00"));

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("MANAGER", assessment.requiredRole());
        assertTrue(assessment.description().contains("Desconto Comercial"));
    }

    @Test
    @DisplayName("Desconto Crítico > 25%: Requer ADMIN")
    void desconto_critico30Pct_requerAdmin() {
        Client client = client(CLIENT_ID, "Hotel Bela Vista", new BigDecimal("100000.00"));
        Order order = order("EC-007", client, new BigDecimal("25000.00"), new BigDecimal("30.00"));

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("ADMIN", assessment.requiredRole());
        assertTrue(assessment.description().contains("Desconto Crítico"));
    }

    @Test
    @DisplayName("Alçada de Montante > 50.000 MT e <= 250.000 MT: Requer MANAGER")
    void alcada_valor80k_requerManager() {
        Client client = client(CLIENT_ID, "Cliente Limpo", new BigDecimal("500000.00"));
        Order order = order("EC-008", client, new BigDecimal("80000.00"), BigDecimal.ZERO);

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("MANAGER", assessment.requiredRole());
        assertTrue(assessment.description().contains("Alçada de Valor de Gerência"));
    }

    @Test
    @DisplayName("Alçada de Montante > 250.000 MT: Requer ADMIN")
    void alcada_valor300k_requerAdmin() {
        Client client = client(CLIENT_ID, "Cliente Limpo Grande", new BigDecimal("1000000.00"));
        Order order = order("EC-009", client, new BigDecimal("300000.00"), BigDecimal.ZERO);

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        OrderRiskAssessment assessment = evaluator.evaluate(order, COMPANY_ID);

        assertFalse(assessment.autoApproved());
        assertEquals("ADMIN", assessment.requiredRole());
        assertTrue(assessment.description().contains("Alçada de Valor da Administração"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private Client client(Long id, String name, BigDecimal creditLimit) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
        c.setCreditLimit(creditLimit);
        return c;
    }

    private Order order(String number, Client client, BigDecimal totalAmount, BigDecimal lineDiscount) {
        Order o = new Order();
        o.setOrderNumber(number);
        o.setClient(client);
        o.setTotalAmount(totalAmount);

        Product p = new Product();
        p.setName("Produto Teste");

        OrderLine line = new OrderLine();
        line.setProduct(p);
        line.setDiscountPercentage(lineDiscount);
        line.setUnitPrice(totalAmount);
        line.setQuantity(BigDecimal.ONE);
        o.addLine(line);

        return o;
    }

    private Invoice overdueInvoice(Client client, BigDecimal amount, int daysOverdue) {
        Invoice inv = new Invoice();
        inv.setClient(client);
        inv.setStatus(InvoiceStatus.APPROVED);
        inv.setTotalAmount(amount);
        inv.setAmountPaid(BigDecimal.ZERO);
        inv.setCreatedAt(LocalDate.now().minusDays(daysOverdue + 30).atStartOfDay());
        inv.setDueDate(LocalDate.now().minusDays(daysOverdue));
        return inv;
    }
}

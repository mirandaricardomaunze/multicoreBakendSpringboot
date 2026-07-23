package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.dto.CreateDeliveryGuideRequest;
import com.phcpro.modules.comercial.dto.DeliveryGuideDTO;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.DeliveryGuide;
import com.phcpro.modules.comercial.model.DeliveryGuideLine;
import com.phcpro.modules.comercial.model.DeliveryGuideStatus;
import com.phcpro.modules.comercial.model.Order;
import com.phcpro.modules.comercial.model.OrderLine;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.DeliveryGuideRepository;
import com.phcpro.modules.comercial.repository.OrderRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Máquina de estados da Guia de Remessa ao cliente (Mockito puro — não levanta o Spring).
 * Foco: o stock (SALE) só sai na APROVAÇÃO; só MANAGER/ADMIN aprovam/rejeitam; a encomenda é
 * travada ao gerar a guia e libertada ao rejeitar/cancelar. Ver
 * docs/GUIA_REMESSA_ENCOMENDA_HARNESS.md (GR-01..GR-10).
 */
class DeliveryGuideServiceTest {

    private DeliveryGuideRepository guideRepository;
    private OrderRepository orderRepository;
    private InventoryService inventoryService;
    private DocumentNumberService documentNumberService;
    private AuditLogService auditLogService;
    private DeliveryGuideService service;

    private Company company;
    private Warehouse warehouse;
    private Product product;
    private Client client;

    @BeforeEach
    void setUp() {
        guideRepository = mock(DeliveryGuideRepository.class);
        orderRepository = mock(OrderRepository.class);
        inventoryService = mock(InventoryService.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditLogService = mock(AuditLogService.class);

        service = new DeliveryGuideService(guideRepository, orderRepository, inventoryService,
                documentNumberService, auditLogService);

        company = company(1L);
        warehouse = warehouse(10L, "Loja", company);
        product = product(100L, "SKU-1", "Arroz");
        client = client(200L, "Cliente A");

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("joao", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ────────────────────────── create ──────────────────────────

    @Test
    void create_ficaPendente_travaEncomenda_eNaoMoveStock() { // GR-01, GR-10
        Order order = pendingOrder(new BigDecimal("5"));
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(documentNumberService.next(any())).thenReturn("GR-2026/1");
        when(guideRepository.save(any(DeliveryGuide.class))).thenAnswer(inv -> {
            DeliveryGuide g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        DeliveryGuideDTO dto = service.createFromOrder(new CreateDeliveryGuideRequest(500L, "João", "AAA-11-MC", null));

        assertEquals(DeliveryGuideStatus.PENDING_APPROVAL.name(), dto.status());
        assertEquals("GR-2026/1", dto.guideNumber());
        // A encomenda deixa de ser faturável (caminhos separados).
        assertEquals("GUIDE_PENDING", order.getStatus());
        assertEquals(1L, order.getDeliveryGuideId());
        // Nada de stock na criação.
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
        verify(documentNumberService).next(any());
    }

    @Test
    void create_encomendaNaoPending_lancaExcecao() { // GR-02
        Order order = pendingOrder(new BigDecimal("5"));
        order.setStatus("BILLED");
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class,
                () -> service.createFromOrder(new CreateDeliveryGuideRequest(500L, null, null, null)));
        verify(guideRepository, never()).save(any());
    }

    // ────────────────────────── approve ──────────────────────────

    @Test
    void approve_comAdmin_moveStock_eFicaAprovada_encomendaGuided() { // GR-03
        DeliveryGuide guide = pendingGuide(new BigDecimal("5"));
        Order order = pendingOrder(new BigDecimal("5"));
        order.setStatus("GUIDE_PENDING");
        when(guideRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(guide));
        when(guideRepository.save(any(DeliveryGuide.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        DeliveryGuideDTO dto = service.approve(1L);

        assertEquals(DeliveryGuideStatus.APPROVED.name(), dto.status());
        assertEquals("joao", dto.approvedBy());
        // Uma saída SALE por linha, quantidade negativa, no armazém da guia.
        verify(inventoryService).registerMovement(eq(product), eq(warehouse),
                eq(new BigDecimal("5").negate()), eq("SALE"), any(), any(), any());
        assertEquals("GUIDED", order.getStatus());
    }

    @Test
    void approve_semPerfilAutorizado_lancaExcecao_eNaoMoveStock() { // GR-04
        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        DeliveryGuide guide = pendingGuide(new BigDecimal("5"));
        when(guideRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(guide));

        assertThrows(BusinessRuleException.class, () -> service.approve(1L));
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
        assertEquals(DeliveryGuideStatus.PENDING_APPROVAL, guide.getStatus());
    }

    @Test
    void approve_guiaJaAprovada_lancaExcecao() { // GR-05
        DeliveryGuide guide = pendingGuide(new BigDecimal("5"));
        guide.setStatus(DeliveryGuideStatus.APPROVED);
        when(guideRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(guide));

        assertThrows(BusinessRuleException.class, () -> service.approve(1L));
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    // ────────────────────────── reject / cancel ──────────────────────────

    @Test
    void reject_naoMoveStock_guardaMotivo_eLibertaEncomenda() { // GR-06
        DeliveryGuide guide = pendingGuide(new BigDecimal("5"));
        Order order = pendingOrder(new BigDecimal("5"));
        order.setStatus("GUIDE_PENDING");
        order.setDeliveryGuideId(1L);
        when(guideRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(guide));
        when(guideRepository.save(any(DeliveryGuide.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        DeliveryGuideDTO dto = service.reject(1L, "Cliente desistiu");

        assertEquals(DeliveryGuideStatus.REJECTED.name(), dto.status());
        assertEquals("Cliente desistiu", dto.rejectionReason());
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
        // A encomenda volta a ser faturável.
        assertEquals("PENDING", order.getStatus());
        assertNull(order.getDeliveryGuideId());
    }

    @Test
    void reject_semMotivo_lancaExcecao() { // GR-07
        DeliveryGuide guide = pendingGuide(new BigDecimal("5"));
        when(guideRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(guide));

        assertThrows(BusinessRuleException.class, () -> service.reject(1L, "  "));
    }

    @Test
    void cancel_pendente_ficaCancelada_eLibertaEncomenda() { // GR-08
        DeliveryGuide guide = pendingGuide(new BigDecimal("5"));
        Order order = pendingOrder(new BigDecimal("5"));
        order.setStatus("GUIDE_PENDING");
        order.setDeliveryGuideId(1L);
        when(guideRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(guide));
        when(guideRepository.save(any(DeliveryGuide.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        DeliveryGuideDTO dto = service.cancel(1L);

        assertEquals(DeliveryGuideStatus.CANCELLED.name(), dto.status());
        assertEquals("PENDING", order.getStatus());
        assertNull(order.getDeliveryGuideId());
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancel_aprovada_lancaExcecao() { // GR-09
        DeliveryGuide guide = pendingGuide(new BigDecimal("5"));
        guide.setStatus(DeliveryGuideStatus.APPROVED);
        when(guideRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(guide));

        assertThrows(BusinessRuleException.class, () -> service.cancel(1L));
    }

    // ────────────────────────── helpers ──────────────────────────

    private Order pendingOrder(BigDecimal qty) {
        Order order = new Order();
        order.setId(500L);
        order.setOrderNumber("EC-2026/1");
        order.setStatus("PENDING");
        order.setCompany(company);
        order.setClient(client);
        order.setWarehouse(warehouse);
        order.setTotalAmount(new BigDecimal("100.00"));
        OrderLine line = new OrderLine();
        line.setProduct(product);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("20.00"));
        line.setTaxRate(new BigDecimal("0.1600"));
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("100.00"));
        order.addLine(line);
        return order;
    }

    private DeliveryGuide pendingGuide(BigDecimal qty) {
        DeliveryGuide g = new DeliveryGuide();
        g.setId(1L);
        g.setGuideNumber("GR-2026/1");
        g.setCompany(company);
        g.setOrderId(500L);
        g.setOrderNumber("EC-2026/1");
        g.setClient(client);
        g.setWarehouse(warehouse);
        g.setStatus(DeliveryGuideStatus.PENDING_APPROVAL);
        DeliveryGuideLine line = new DeliveryGuideLine();
        line.setGuide(g);
        line.setProduct(product);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("20.00"));
        line.setTaxRate(new BigDecimal("0.1600"));
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("100.00"));
        g.getLines().add(line);
        return g;
    }

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Warehouse warehouse(long id, String name, Company company) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setName(name);
        w.setCompany(company);
        return w;
    }

    private static Product product(long id, String sku, String name) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        return p;
    }

    private static Client client(long id, String name) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
        return c;
    }
}

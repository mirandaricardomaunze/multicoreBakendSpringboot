package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.OrderEventRepository;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.printing.OrderPickingPrintService;
import mz.multicore.erp.modules.printing.OrderPickingSheetPrintService;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPickingConferenciaHarnessTest {

    private ComercialService comercialService;
    private OrderRepository orderRepository;
    private OrderLineRepository orderLineRepository;
    private OrderEventRepository eventRepository;
    private InventoryService inventoryService;
    private OrderPickingPrintService printService;
    private OrderPickingSheetPrintService sheetPrintService;
    private AppUserService appUserService;
    private CustomerOrderFulfillmentService service;

    private Company company;
    private Warehouse warehouse;
    private Client client;

    @BeforeEach
    void setup() {
        comercialService = mock(ComercialService.class);
        orderRepository = mock(OrderRepository.class);
        orderLineRepository = mock(OrderLineRepository.class);
        eventRepository = mock(OrderEventRepository.class);
        inventoryService = mock(InventoryService.class);
        printService = mock(OrderPickingPrintService.class);
        sheetPrintService = mock(OrderPickingSheetPrintService.class);
        appUserService = mock(AppUserService.class);

        service = new CustomerOrderFulfillmentService(
                comercialService, orderRepository, orderLineRepository, eventRepository,
                inventoryService, printService, sheetPrintService, appUserService
        );

        company = new Company();
        company.setId(1L);
        company.setName("Distribuidora Global");

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Armazém Central");

        client = new Client();
        client.setId(20L);
        client.setName("Mercearia Maputo");

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("conferidor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("getPickingDetails calcula correctamente as caixas e transporta códigos de barras")
    void getPickingDetails_calculatesBoxesAndPackaging() {
        Product p = new Product();
        p.setId(5L);
        p.setName("Leite Condensado");
        p.setReference("LEI-01");
        p.setBarcode("5601234567890");
        p.setPackagesPerBox(24);
        p.setUnitsPerPackage(1);

        OrderLine line = new OrderLine();
        line.setId(101L);
        line.setProduct(p);
        line.setQuantity(new BigDecimal("48")); // 2 caixas completas
        line.setSeparatedQuantity(BigDecimal.ZERO);

        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ENC-001");
        order.setStatus("AWAITING_SEPARATION");
        order.setCompany(company);
        order.setWarehouse(warehouse);
        order.setClient(client);
        order.setLines(new ArrayList<>(List.of(line)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderPickingDetailsDTO details = service.getPickingDetails(1L);
        assertNotNull(details);
        assertEquals("ENC-001", details.orderNumber());
        assertEquals("Mercearia Maputo", details.clientName());
        assertEquals(1, details.lines().size());

        OrderPickingLineDTO item = details.lines().get(0);
        assertEquals("LEI-01", item.productReference());
        assertEquals("5601234567890", item.barcode());
        assertEquals(new BigDecimal("48"), item.requestedQuantity());
        assertEquals(new BigDecimal("2.00"), item.boxes());
        assertEquals(new BigDecimal("2.00"), details.totalBoxes());
    }

    @Test
    @DisplayName("completeSeparationWithConfirmation actualiza quantidades conferidas e lotes bipados")
    void completeSeparationWithConfirmation_updatesQuantitiesAndBatches() {
        Product p = new Product();
        p.setId(5L);
        p.setName("Óleo 1L");
        p.setPackagesPerBox(4);
        p.setUnitsPerPackage(1);

        OrderLine line = new OrderLine();
        line.setId(101L);
        line.setProduct(p);
        line.setQuantity(new BigDecimal("12"));
        line.setSeparatedQuantity(BigDecimal.ZERO);

        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ENC-001");
        order.setStatus("IN_SEPARATION");
        order.setKind(OrderKind.PICKING_REQUEST);
        order.setCompany(company);
        order.setWarehouse(warehouse);
        order.setClient(client);
        order.setLines(new ArrayList<>(List.of(line)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderDTO mockDto = mock(OrderDTO.class);
        when(comercialService.toDTO(any(Order.class))).thenReturn(mockDto);

        CompleteOrderPickingRequest request = new CompleteOrderPickingRequest(
                1L,
                "LEITOR-01",
                "Conferido com sucesso na palete 3",
                List.of(new OrderPickingConfirmationLineRequest(101L, new BigDecimal("12"), "LOTE-NOV-26"))
        );

        OrderDTO result = service.completeSeparationWithConfirmation(request);
        assertEquals("SEPARATED", order.getStatus());
        assertEquals(new BigDecimal("12"), line.getSeparatedQuantity());
        assertEquals("LOTE-NOV-26", line.getBatchNumber());
        verify(orderRepository).save(order);
        verify(eventRepository).save(any(OrderEvent.class));
    }

    @Test
    @DisplayName("completeSeparationWithConfirmation sem linhas explícitas assume 100% separado")
    void completeSeparationWithConfirmation_emptyLinesAssumesFullQuantity() {
        Product p = new Product();
        p.setId(5L);
        p.setName("Arroz");

        OrderLine line = new OrderLine();
        line.setId(101L);
        line.setProduct(p);
        line.setQuantity(new BigDecimal("10"));

        Order order = new Order();
        order.setId(1L);
        order.setStatus("IN_SEPARATION");
        order.setKind(OrderKind.PICKING_REQUEST);
        order.setCompany(company);
        order.setLines(new ArrayList<>(List.of(line)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderDTO mockDto = mock(OrderDTO.class);
        when(comercialService.toDTO(any(Order.class))).thenReturn(mockDto);

        CompleteOrderPickingRequest request = new CompleteOrderPickingRequest(1L, "TERM-01");
        service.completeSeparationWithConfirmation(request);

        assertEquals("SEPARATED", order.getStatus());
        assertEquals(new BigDecimal("10"), line.getSeparatedQuantity());
    }

    @Test
    @DisplayName("completeSeparationWithConfirmation rejeita encomenda já facturada")
    void completeSeparationWithConfirmation_rejectsInvoicedOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("INVOICED");
        order.setKind(OrderKind.PICKING_REQUEST);
        order.setCompany(company);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        CompleteOrderPickingRequest request = new CompleteOrderPickingRequest(1L, "TERM-01");
        assertThrows(BusinessRuleException.class, () -> service.completeSeparationWithConfirmation(request));
    }

    // ─── Circuito fechado à encomenda A4 e conferência completa ─────────────

    private Order pickingOrder(OrderKind kind, String status, OrderLine... lines) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ENC-009");
        order.setKind(kind);
        order.setStatus(status);
        order.setCompany(company);
        order.setWarehouse(warehouse);
        order.setClient(client);
        order.setLines(new ArrayList<>(List.of(lines)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(comercialService.toDTO(any(Order.class))).thenReturn(mock(OrderDTO.class));
        return order;
    }

    private OrderLine line(long id, String name, String quantity) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setProduct(p);
        line.setQuantity(new BigDecimal(quantity));
        return line;
    }

    @Test
    @DisplayName("conferência recusa encomenda A4 aprovada — não passa pelo armazém")
    void conferencia_encomendaA4Pendente_recusaENaoMudaEstado() {
        Order order = pickingOrder(OrderKind.FORMAL_ORDER, "PENDING", line(101L, "Arroz", "10"));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.completeSeparationWithConfirmation(new CompleteOrderPickingRequest(1L, "TERM-01")));

        assertTrue(error.getMessage().contains("não passa pelo armazém"), error.getMessage());
        assertEquals("PENDING", order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("conferência aceita zero numa linha esgotada")
    void conferencia_linhaComZero_ficaSeparadaComZero() {
        OrderLine arroz = line(101L, "Arroz", "10");
        OrderLine oleo = line(102L, "Óleo", "5");
        Order order = pickingOrder(OrderKind.PICKING_REQUEST, "IN_SEPARATION", arroz, oleo);

        service.completeSeparationWithConfirmation(new CompleteOrderPickingRequest(1L, "TERM-01", null, List.of(
                new OrderPickingConfirmationLineRequest(101L, new BigDecimal("10"), null),
                new OrderPickingConfirmationLineRequest(102L, BigDecimal.ZERO, null))));

        assertEquals("SEPARATED", order.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(oleo.getSeparatedQuantity()));
    }

    @Test
    @DisplayName("conferência parcial liberta a reserva do que não saiu")
    void conferenciaParcial_reservaPassaAQuantidadeSeparada() {
        OrderLine arroz = line(101L, "Arroz", "10");
        arroz.setReservedQuantity(new BigDecimal("10"));
        Order order = pickingOrder(OrderKind.PICKING_REQUEST, "IN_SEPARATION", arroz);
        order.setReservationActive(true);

        service.completeSeparationWithConfirmation(new CompleteOrderPickingRequest(1L, "TERM-01", null,
                List.of(new OrderPickingConfirmationLineRequest(101L, new BigDecimal("6"), null))));

        // As 4 que ficaram na prateleira voltam a estar livres para outras vendas.
        assertEquals(0, new BigDecimal("6").compareTo(arroz.getReservedQuantity()));
    }

    @Test
    @DisplayName("conferência recusa linha esquecida em vez de a dar como separado zero")
    void conferencia_linhaEmFalta_recusa() {
        Order order = pickingOrder(OrderKind.PICKING_REQUEST, "IN_SEPARATION",
                line(101L, "Arroz", "10"), line(102L, "Óleo", "5"));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.completeSeparationWithConfirmation(new CompleteOrderPickingRequest(1L, "TERM-01", null,
                        List.of(new OrderPickingConfirmationLineRequest(101L, new BigDecimal("10"), null)))));

        assertTrue(error.getMessage().contains("Óleo"), error.getMessage());
        assertEquals("IN_SEPARATION", order.getStatus());
    }

    @Test
    @DisplayName("conferência recusa linha que não pertence à encomenda")
    void conferencia_linhaDeOutraEncomenda_recusa() {
        pickingOrder(OrderKind.PICKING_REQUEST, "IN_SEPARATION", line(101L, "Arroz", "10"));

        assertThrows(BusinessRuleException.class,
                () -> service.completeSeparationWithConfirmation(new CompleteOrderPickingRequest(1L, "TERM-01", null, List.of(
                        new OrderPickingConfirmationLineRequest(101L, new BigDecimal("10"), null),
                        new OrderPickingConfirmationLineRequest(999L, BigDecimal.ONE, null)))));
    }

    @Test
    @DisplayName("folha A4 de separação recusa encomenda A4 e não a põe em separação")
    void folhaA4_encomendaFormal_recusaENaoMudaEstado() {
        Order order = pickingOrder(OrderKind.FORMAL_ORDER, "PENDING", line(101L, "Arroz", "10"));

        assertThrows(BusinessRuleException.class, () -> service.printForPickingA4(1L, "TERM-01"));

        assertEquals("PENDING", order.getStatus());
        verify(sheetPrintService, never()).renderA4(anyLong(), anyBoolean());
    }
}

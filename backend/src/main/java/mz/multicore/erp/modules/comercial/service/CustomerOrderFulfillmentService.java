package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.OrderEventRepository;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.printing.OrderPickingPrintService;
import mz.multicore.erp.modules.printing.OrderPickingSheetPrintService;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquestrador do circuito de separação e expedição de encomendas de clientes.
 * Delega a coordenação de impressão e o ciclo de vida para colaboradores dedicados.
 */
@Service
public class CustomerOrderFulfillmentService {

    private final ComercialService comercialService;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderEventRepository eventRepository;
    private final InventoryService inventoryService;
    private final OrderPickingCoordinator pickingCoordinator;
    private final OrderFulfillmentLifecycleService lifecycleService;

    @Autowired
    public CustomerOrderFulfillmentService(
            ComercialService comercialService,
            OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            OrderEventRepository eventRepository,
            InventoryService inventoryService,
            OrderPickingCoordinator pickingCoordinator,
            OrderFulfillmentLifecycleService lifecycleService) {
        this.comercialService = comercialService;
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.eventRepository = eventRepository;
        this.inventoryService = inventoryService;
        this.pickingCoordinator = pickingCoordinator;
        this.lifecycleService = lifecycleService;
    }

    /** Construtor retrocompatível de 8 dependências. */
    public CustomerOrderFulfillmentService(
            ComercialService comercialService, OrderRepository orderRepository,
            OrderLineRepository orderLineRepository, OrderEventRepository eventRepository,
            InventoryService inventoryService, OrderPickingPrintService printService,
            OrderPickingSheetPrintService sheetPrintService, AppUserService appUserService) {
        this(comercialService, orderRepository, orderLineRepository, eventRepository, inventoryService,
                new OrderPickingCoordinator(orderRepository, eventRepository, printService, sheetPrintService, appUserService),
                new OrderFulfillmentLifecycleService(comercialService, orderRepository, eventRepository,
                        new OrderPickingCoordinator(orderRepository, eventRepository, printService, sheetPrintService, appUserService)));
    }

    /** Construtor retrocompatível de 7 dependências para testes legados. */
    public CustomerOrderFulfillmentService(
            ComercialService comercialService, OrderRepository orderRepository,
            OrderLineRepository orderLineRepository, OrderEventRepository eventRepository,
            InventoryService inventoryService, OrderPickingPrintService printService, AppUserService appUserService) {
        this(comercialService, orderRepository, orderLineRepository, eventRepository, inventoryService,
                printService, new OrderPickingSheetPrintService(orderRepository), appUserService);
    }

    @Transactional
    public OrderDTO submit(CreateFulfillmentOrderRequest request) {
        PermissionGuard.requireSeller("registar pedidos de clientes");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CurrentUserContext.requireCompany(request.order().companyId());
        String key = request.idempotencyKey().trim();
        return orderRepository.findByCompanyIdAndIdempotencyKey(companyId, key)
                .map(comercialService::toDTO)
                .orElseGet(() -> createAndReserve(request, key));
    }

    private OrderDTO createAndReserve(CreateFulfillmentOrderRequest request, String key) {
        OrderDTO created = comercialService.createOrder(request.order(), OrderKind.PICKING_REQUEST);
        Order order = pickingCoordinator.requireOrder(created.id());
        // Somado por artigo: duas linhas do mesmo produto não podem, cada uma, caber no stock livre.
        OrderStockAvailability.require(order, order.getWarehouse(), order.getLines().stream()
                .map(l -> new OrderStockAvailability.Outgoing(l.getProduct(), l.getQuantity())).toList(),
                inventoryService, orderLineRepository);
        order.getLines().forEach(line -> line.setReservedQuantity(line.getQuantity()));
        order.setIdempotencyKey(key);
        order.setReservationActive(true);
        String previous = order.getStatus();
        order.setStatus(OrderFulfillmentStatus.AWAITING_SEPARATION.name());
        orderRepository.save(order);
        pickingCoordinator.recordEvent(order, "ORDER_SUBMITTED_STOCK_RESERVED", previous, order.getStatus(),
                request.terminalName(), "Pedido enviado para a central; stock reservado.");
        return comercialService.toDTO(order);
    }

    @Transactional
    public byte[] printForPicking(Long id, String terminal) {
        return pickingCoordinator.printForPicking(id, terminal);
    }

    @Transactional
    public byte[] reprint(Long id, ReprintAuthorizationRequest request) {
        return pickingCoordinator.reprint(id, request);
    }

    @Transactional(readOnly = true)
    public OrderPickingDetailsDTO getPickingDetails(Long id) {
        return pickingCoordinator.getPickingDetails(id);
    }

    @Transactional
    public byte[] printForPickingA4(Long id, String terminal) {
        return pickingCoordinator.printForPickingA4(id, terminal);
    }

    @Transactional
    public OrderDTO completeSeparationWithConfirmation(CompleteOrderPickingRequest request) {
        return lifecycleService.completeSeparationWithConfirmation(request);
    }

    @Transactional
    public OrderDTO completeSeparation(Long id, OrderActionRequest request) {
        return lifecycleService.completeSeparation(id, request);
    }

    @Transactional
    public InvoiceDTO bill(Long id, OrderActionRequest request) {
        return lifecycleService.bill(id, request);
    }

    @Transactional(readOnly = true)
    public List<OrderEventDTO> events(Long id) {
        pickingCoordinator.requireOrder(id);
        return eventRepository.findByOrderIdOrderByCreatedAtAsc(id).stream().map(e -> new OrderEventDTO(
                e.getId(), e.getEventType(), e.getPreviousStatus(), e.getNewStatus(), e.getCreatedBy(),
                e.getActorRole(), e.getTerminalName(), e.getDetails(), e.getCreatedAt())).toList();
    }
}

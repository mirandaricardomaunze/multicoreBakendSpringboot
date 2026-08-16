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
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.service.AppUserService;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CustomerOrderFulfillmentService {
    private final ComercialService comercialService;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderEventRepository eventRepository;
    private final InventoryService inventoryService;
    private final OrderPickingPrintService printService;
    private final AppUserService appUserService;
    private final ApprovalService approvalService;

    public CustomerOrderFulfillmentService(ComercialService comercialService, OrderRepository orderRepository,
            OrderLineRepository orderLineRepository, OrderEventRepository eventRepository,
            InventoryService inventoryService, OrderPickingPrintService printService, AppUserService appUserService,
            ApprovalService approvalService) {
        this.comercialService = comercialService;
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.eventRepository = eventRepository;
        this.inventoryService = inventoryService;
        this.printService = printService;
        this.appUserService = appUserService;
        this.approvalService = approvalService;
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
        OrderDTO created = comercialService.createOrder(request.order());
        Order order = requireOrder(created.id());
        approvalService.cancelPendingForDocument("ORDER", order.getId(),
                "Pedido encaminhado para o fluxo operacional de separacao.");
        for (OrderLine line : order.getLines()) {
            BigDecimal physical = inventoryService.lockAndGetPhysicalQuantity(line.getProduct().getId(), order.getWarehouse().getId());
            BigDecimal reserved = orderLineRepository.sumActiveReservations(line.getProduct().getId(), order.getWarehouse().getId());
            if (physical.subtract(reserved).compareTo(line.getQuantity()) < 0) {
                throw new BusinessRuleException("Stock disponivel insuficiente para " + line.getProduct().getName() + ".");
            }
            line.setReservedQuantity(line.getQuantity());
        }
        order.setIdempotencyKey(key);
        order.setReservationActive(true);
        String previous = order.getStatus();
        order.setStatus(OrderFulfillmentStatus.AWAITING_SEPARATION.name());
        orderRepository.save(order);
        record(order, "ORDER_SUBMITTED_STOCK_RESERVED", previous, order.getStatus(), request.terminalName(),
                "Pedido enviado para a central; stock reservado.");
        return comercialService.toDTO(order);
    }

    @Transactional
    public byte[] printForPicking(Long id, String terminal) {
        PermissionGuard.requireManagerOrAdmin("imprimir guia de separacao");
        Order order = requireOrder(id);
        transition(order, OrderFulfillmentStatus.AWAITING_SEPARATION, OrderFulfillmentStatus.IN_SEPARATION);
        byte[] document = printService.render(id, false);
        registerPrint(order, terminal, "PICKING_PRINTED", "Primeira impressao; separacao iniciada.");
        return document;
    }

    @Transactional
    public byte[] reprint(Long id, ReprintAuthorizationRequest request) {
        PermissionGuard.requireManagerOrAdmin("reimprimir guia de separacao");
        Order order = requireOrder(id);
        if (!OrderFulfillmentStatus.IN_SEPARATION.name().equals(order.getStatus())) {
            throw new BusinessRuleException("A guia so pode ser reimpressa durante a separacao.");
        }
        if (CurrentUserContext.getUsername().equalsIgnoreCase(request.approverUsername())) {
            throw new BusinessRuleException("A reimpressao exige autorizacao de outro utilizador.");
        }
        AppUser approver = appUserService.authenticate(request.approverUsername(), request.approverPassword());
        AppUserCompanyAccess access = approver.findCompanyAccess(CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("O autorizador nao pertence a empresa activa."));
        if (!"MANAGER".equalsIgnoreCase(access.getRole()) && !"ADMIN".equalsIgnoreCase(access.getRole())) {
            throw new BusinessRuleException("O autorizador deve ser gerente ou administrador.");
        }
        byte[] document = printService.render(id, true);
        registerPrint(order, request.terminalName(), "PICKING_REPRINTED",
                "Autorizado por " + approver.getUsername() + ". Motivo: " + request.reason().trim());
        return document;
    }

    @Transactional
    public OrderDTO completeSeparation(Long id, OrderActionRequest request) {
        PermissionGuard.requireManagerOrAdmin("concluir separacao");
        Order order = requireOrder(id);
        transition(order, OrderFulfillmentStatus.IN_SEPARATION, OrderFulfillmentStatus.SEPARATED);
        order.getLines().forEach(line -> line.setSeparatedQuantity(line.getQuantity()));
        orderRepository.save(order);
        record(order, "SEPARATION_COMPLETED", OrderFulfillmentStatus.IN_SEPARATION.name(), order.getStatus(),
                request.terminalName(), "Todos os artigos foram separados e conferidos.");
        return comercialService.toDTO(order);
    }

    @Transactional
    public InvoiceDTO bill(Long id, OrderActionRequest request) {
        PermissionGuard.requireManagerOrAdmin("facturar pedido separado");
        Order order = requireOrder(id);
        if (!OrderFulfillmentStatus.SEPARATED.name().equals(order.getStatus()) || order.getInvoiceId() != null) {
            throw new BusinessRuleException("Apenas um pedido separado e ainda nao facturado pode ser facturado.");
        }
        InvoiceDTO invoice = comercialService.billOrder(id);
        order = requireOrder(id);
        order.setReservationActive(false);
        order.setStatus(OrderFulfillmentStatus.INVOICED.name());
        orderRepository.save(order);
        record(order, "ORDER_INVOICED_STOCK_DEDUCTED", OrderFulfillmentStatus.SEPARATED.name(), order.getStatus(),
                request.terminalName(), "Fatura " + invoice.invoiceNumber() + "; reserva consumida e stock deduzido.");
        return invoice;
    }

    @Transactional(readOnly = true)
    public List<OrderEventDTO> events(Long id) {
        requireOrder(id);
        return eventRepository.findByOrderIdOrderByCreatedAtAsc(id).stream().map(e -> new OrderEventDTO(
                e.getId(), e.getEventType(), e.getPreviousStatus(), e.getNewStatus(), e.getCreatedBy(),
                e.getActorRole(), e.getTerminalName(), e.getDetails(), e.getCreatedAt())).toList();
    }

    private Order requireOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new BusinessRuleException("Pedido nao encontrado."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        order.getLines().size();
        return order;
    }

    private void transition(Order order, OrderFulfillmentStatus expected, OrderFulfillmentStatus target) {
        if (!expected.name().equals(order.getStatus())) throw new BusinessRuleException("Estado actual invalido: " + order.getStatus() + ".");
        expected.requireTransitionTo(target);
        order.setStatus(target.name());
    }

    private void registerPrint(Order order, String terminal, String type, String details) {
        order.setPrintCount(order.getPrintCount() + 1);
        order.setPrintedAt(java.time.LocalDateTime.now());
        order.setLastPrintedBy(CurrentUserContext.getUsername());
        orderRepository.save(order);
        record(order, type, order.getStatus(), order.getStatus(), terminal, details);
    }

    private void record(Order order, String type, String previous, String next, String terminal, String details) {
        OrderEvent event = new OrderEvent();
        event.setOrder(order); event.setEventType(type); event.setPreviousStatus(previous); event.setNewStatus(next);
        event.setActorRole(CurrentUserContext.getRole()); event.setTerminalName(terminal); event.setDetails(details);
        event.setCreatedBy(CurrentUserContext.getUsername());
        eventRepository.save(event);
    }
}

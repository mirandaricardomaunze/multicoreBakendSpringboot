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

    public CustomerOrderFulfillmentService(ComercialService comercialService, OrderRepository orderRepository,
            OrderLineRepository orderLineRepository, OrderEventRepository eventRepository,
            InventoryService inventoryService, OrderPickingPrintService printService, AppUserService appUserService) {
        this.comercialService = comercialService;
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.eventRepository = eventRepository;
        this.inventoryService = inventoryService;
        this.printService = printService;
        this.appUserService = appUserService;
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
        // A via é forçada aqui, não lida do pedido HTTP: este circuito é dono da sua via e não a
        // pode deixar ao critério de quem chama. Como PICKING_REQUEST não cria aprovação nenhuma,
        // deixou de ser preciso criar uma e cancelá-la a seguir.
        OrderDTO created = comercialService.createOrder(request.order(), OrderKind.PICKING_REQUEST);
        Order order = requireOrder(created.id());
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
        Order order = requireSeparationCircuit(id, "imprimir a lista de separação");
        transition(order, OrderFulfillmentStatus.AWAITING_SEPARATION, OrderFulfillmentStatus.IN_SEPARATION);
        byte[] document = printService.render(id, false);
        registerPrint(order, terminal, "PICKING_PRINTED", "Primeira impressao; separacao iniciada.");
        return document;
    }

    @Transactional
    public byte[] reprint(Long id, ReprintAuthorizationRequest request) {
        PermissionGuard.requireManagerOrAdmin("reimprimir guia de separacao");
        Order order = requireSeparationCircuit(id, "reimprimir a lista de separação");
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
        Order order = requireSeparationCircuit(id, "marcar como separado");
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

    /**
     * Recusa uma encomenda que não é deste circuito, <b>antes</b> de olhar para o estado.
     *
     * <p>Uma encomenda A4 nunca esteve em separação, pelo que a recusa por estado ("ainda aguarda
     * separação") mandaria o operador imprimir uma lista que não existe. A via é a razão certa e
     * é a que se diz.
     */
    private Order requireSeparationCircuit(Long id, String action) {
        Order order = requireOrder(id);
        if (!OrderKind.orDefault(order.getKind()).usesSeparationFlow()) {
            throw new BusinessRuleException("Não é possível " + action + ": a encomenda "
                    + order.getOrderNumber() + " é do tipo \"" + OrderKind.FORMAL_ORDER.label()
                    + "\" e não passa pelo armazém — aprova-se e fatura-se directamente.");
        }
        return order;
    }

    /**
     * Muda o estado do pedido, recusando quando o passo anterior não foi dado.
     *
     * <p>A mensagem diz <b>o que fazer</b> e não só o que está errado. "Estado actual invalido:
     * PENDING" é verdade e não serve de nada a quem está ao balcão — o operador precisa de saber
     * que lhe falta imprimir a lista de separação, ou que aquela encomenda nem sequer faz parte
     * deste circuito.
     */
    private void transition(Order order, OrderFulfillmentStatus expected, OrderFulfillmentStatus target) {
        if (!expected.name().equals(order.getStatus())) {
            throw new BusinessRuleException(explainWrongState(order, expected, target));
        }
        expected.requireTransitionTo(target);
        order.setStatus(target.name());
    }

    private String explainWrongState(Order order, OrderFulfillmentStatus expected, OrderFulfillmentStatus target) {
        String current = order.getStatus();
        String header = "Não é possível marcar como " + label(target) + ": ";

        // Encomenda do fluxo clássico — nunca passou pelo circuito de separação.
        if (!OrderFulfillmentStatus.isFulfillmentStatus(current)) {
            return header + "esta encomenda (" + order.getOrderNumber() + ") não foi criada para "
                    + "separação — está em \"" + current + "\" e fatura-se directamente.";
        }
        if (OrderFulfillmentStatus.AWAITING_SEPARATION.name().equals(current)) {
            return header + "a encomenda " + order.getOrderNumber() + " ainda aguarda separação. "
                    + "Imprima primeiro a lista de separação — é isso que dá início ao trabalho no armazém.";
        }
        return header + "a encomenda " + order.getOrderNumber() + " está em \"" + label(current)
                + "\" e o passo esperado era \"" + label(expected) + "\".";
    }

    private String label(OrderFulfillmentStatus status) {
        return label(status.name());
    }

    /**
     * Delega na fonte única. Esta tradução chegou a viver aqui, privada e só com os estados de
     * separação — o A4 e os restantes ecrãs ficavam a ver códigos internos. Ver
     * {@link mz.multicore.erp.modules.comercial.model.OrderStatusLabel}.
     */
    private String label(String status) {
        return mz.multicore.erp.modules.comercial.model.OrderStatusLabel.of(status);
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

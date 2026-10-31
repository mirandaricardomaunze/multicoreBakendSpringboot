package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.OrderEventRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão do ciclo de vida e transições de estado do circuito de separação de encomendas.
 */
@Service
public class OrderFulfillmentLifecycleService {

    private final ComercialService comercialService;
    private final OrderRepository orderRepository;
    private final OrderEventRepository eventRepository;
    private final OrderPickingCoordinator pickingCoordinator;

    public OrderFulfillmentLifecycleService(ComercialService comercialService,
                                           OrderRepository orderRepository,
                                           OrderEventRepository eventRepository,
                                           OrderPickingCoordinator pickingCoordinator) {
        this.comercialService = comercialService;
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.pickingCoordinator = pickingCoordinator;
    }

    @Transactional
    public OrderDTO completeSeparationWithConfirmation(CompleteOrderPickingRequest request) {
        PermissionGuard.requireManagerOrAdmin("concluir separacao com conferencia");
        Order order = pickingCoordinator.requireOrder(request.orderId());
        if (!OrderFulfillmentStatus.IN_SEPARATION.name().equals(order.getStatus())
                && !OrderFulfillmentStatus.AWAITING_SEPARATION.name().equals(order.getStatus())
                && !"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("Apenas encomendas pendentes ou em separação podem ser conferidas.");
        }

        if (request.lines() != null && !request.lines().isEmpty()) {
            for (OrderPickingConfirmationLineRequest cl : request.lines()) {
                order.getLines().stream()
                        .filter(l -> l.getId().equals(cl.lineId()))
                        .findFirst()
                        .ifPresent(l -> {
                            l.setSeparatedQuantity(cl.confirmedQuantity() != null ? cl.confirmedQuantity() : l.getQuantity());
                            if (cl.batchNumber() != null && !cl.batchNumber().isBlank()) {
                                l.setBatchNumber(cl.batchNumber().trim());
                            }
                        });
            }
        } else {
            order.getLines().forEach(line -> line.setSeparatedQuantity(line.getQuantity()));
        }

        String previous = order.getStatus();
        order.setStatus(OrderFulfillmentStatus.SEPARATED.name());
        orderRepository.save(order);
        String details = "Conferência óptica de separação concluída."
                + (request.notes() != null && !request.notes().isBlank() ? " Notas: " + request.notes().trim() : "");
        pickingCoordinator.recordEvent(order, "SEPARATION_VERIFIED", previous, order.getStatus(), request.terminalName(), details);
        return comercialService.toDTO(order);
    }

    @Transactional
    public OrderDTO completeSeparation(Long id, OrderActionRequest request) {
        PermissionGuard.requireManagerOrAdmin("concluir separacao");
        Order order = pickingCoordinator.requireSeparationCircuit(id, "marcar como separado");
        transition(order, OrderFulfillmentStatus.IN_SEPARATION, OrderFulfillmentStatus.SEPARATED);
        order.getLines().forEach(line -> line.setSeparatedQuantity(line.getQuantity()));
        orderRepository.save(order);
        pickingCoordinator.recordEvent(order, "SEPARATION_COMPLETED", OrderFulfillmentStatus.IN_SEPARATION.name(),
                order.getStatus(), request.terminalName(), "Todos os artigos foram separados e conferidos.");
        return comercialService.toDTO(order);
    }

    @Transactional
    public InvoiceDTO bill(Long id, OrderActionRequest request) {
        PermissionGuard.requireManagerOrAdmin("facturar pedido separado");
        Order order = pickingCoordinator.requireOrder(id);
        if (!OrderFulfillmentStatus.SEPARATED.name().equals(order.getStatus()) || order.getInvoiceId() != null) {
            throw new BusinessRuleException("Apenas um pedido separado e ainda nao facturado pode ser facturado.");
        }
        InvoiceDTO invoice = comercialService.billOrder(id);
        order = pickingCoordinator.requireOrder(id);
        order.setReservationActive(false);
        order.setStatus(OrderFulfillmentStatus.INVOICED.name());
        orderRepository.save(order);
        pickingCoordinator.recordEvent(order, "ORDER_INVOICED_STOCK_DEDUCTED", OrderFulfillmentStatus.SEPARATED.name(),
                order.getStatus(), request.terminalName(), "Fatura " + invoice.invoiceNumber() + "; reserva consumida e stock deduzido.");
        return invoice;
    }

    private void transition(Order order, OrderFulfillmentStatus expected, OrderFulfillmentStatus target) {
        if (!expected.name().equals(order.getStatus())) {
            throw new BusinessRuleException(explainWrongState(order, expected, target));
        }
        expected.requireTransitionTo(target);
        order.setStatus(target.name());
    }

    private String explainWrongState(Order order, OrderFulfillmentStatus expected, OrderFulfillmentStatus target) {
        String current = order.getStatus();
        String header = "Não é possível marcar como " + OrderStatusLabel.of(target.name()) + ": ";
        if (!OrderFulfillmentStatus.isFulfillmentStatus(current)) {
            return header + "esta encomenda (" + order.getOrderNumber() + ") não foi criada para "
                    + "separação — está em \"" + current + "\" e fatura-se directamente.";
        }
        if (OrderFulfillmentStatus.AWAITING_SEPARATION.name().equals(current)) {
            return header + "a encomenda " + order.getOrderNumber() + " ainda aguarda separação. "
                    + "Imprima primeiro a lista de separação — é isso que dá início ao trabalho no armazém.";
        }
        return header + "a encomenda " + order.getOrderNumber() + " está em \"" + OrderStatusLabel.of(current)
                + "\" e o passo esperado era \"" + OrderStatusLabel.of(expected.name()) + "\".";
    }
}

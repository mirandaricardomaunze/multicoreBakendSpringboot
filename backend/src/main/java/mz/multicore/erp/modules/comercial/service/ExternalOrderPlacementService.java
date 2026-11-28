package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.model.ExternalOrderSource;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Encomendas de sistemas externos (API pública, loja online — docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4): nascem como
 * {@link OrderKind#FORMAL_ORDER} com as regras e a aprovação do desktop e, além disso, reservam logo o stock
 * disponível — a loja online não pode vender o que já está prometido a outro pedido. Sem stock livre, nada fica
 * gravado. A reserva liberta-se ao facturar, cancelar ou rejeitar, como nos pedidos de separação.
 */
@Service
@Transactional
public class ExternalOrderPlacementService {

    private final ComercialOrderLifecycleService orderLifecycleService;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final InventoryService inventoryService;

    public ExternalOrderPlacementService(ComercialOrderLifecycleService orderLifecycleService,
                                         OrderRepository orderRepository, OrderLineRepository orderLineRepository,
                                         @Lazy InventoryService inventoryService) {
        this.orderLifecycleService = orderLifecycleService;
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.inventoryService = inventoryService;
    }

    /** @throws InsufficientStockException quando o stock livre de um artigo não chega (a transacção é desfeita) */
    public OrderDTO placeReservedFormalOrder(CreateOrderRequest request, ExternalOrderSource source) {
        OrderDTO created = orderLifecycleService.createOrder(request, OrderKind.FORMAL_ORDER);
        Order order = orderRepository.findById(created.id()).orElseThrow();
        OrderStockAvailability.requireAndReserve(order, inventoryService, orderLineRepository);
        order.setOrigin(source.origin());
        order.setOriginName(source.originName());
        order.setExternalReference(source.externalReference());
        order.setIdempotencyKey(source.idempotencyKey());
        order.setRequestFingerprint(source.requestFingerprint());
        order.setContactPhone(source.contactPhone());
        order.setFulfilmentLabel(source.fulfilmentLabel());
        order.setDeliveryAddress(source.deliveryAddress());
        return orderLifecycleService.toDTO(orderRepository.save(order));
    }

    /** A encomenda externa já criada com esta chave de idempotência na empresa (SPEC §3.4.2), se existir. */
    @Transactional(readOnly = true)
    public Optional<PlacedExternalOrder> findPlaced(Long companyId, String idempotencyKey) {
        return orderRepository.findByCompanyIdAndIdempotencyKey(companyId, idempotencyKey)
                .map(order -> new PlacedExternalOrder(orderLifecycleService.toDTO(order), order.getRequestFingerprint()));
    }
}

package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consulta de uma encomenda de cliente pelo número, para sistemas externos (docs/API_PUBLICA_WEBHOOKS_SPEC.md
 * §3.4.3): qualquer Encomenda A4 ou Pedido de separação da empresa, venha da API ou do desktop, com o número da
 * factura associada. As reposições internas não são encomendas de cliente e ficam de fora.
 */
@Service
@Transactional(readOnly = true)
public class CustomerOrderTrackingService {

    private final OrderRepository orderRepository;
    private final ComercialOrderLifecycleService orderLifecycleService;
    private final InvoiceLookup invoiceLookup;

    public CustomerOrderTrackingService(OrderRepository orderRepository,
                                        ComercialOrderLifecycleService orderLifecycleService,
                                        InvoiceLookup invoiceLookup) {
        this.orderRepository = orderRepository;
        this.orderLifecycleService = orderLifecycleService;
        this.invoiceLookup = invoiceLookup;
    }

    public Optional<TrackedCustomerOrder> findByNumber(Long companyId, String orderNumber) {
        return customerOrder(companyId, orderNumber)
                .map(order -> new TrackedCustomerOrder(orderLifecycleService.toDTO(order), order.getExternalReference(),
                        invoiceLookup.find(order.getInvoiceId()).map(Invoice::getInvoiceNumber).orElse(null)));
    }

    /**
     * The order with this number, only when it came from {@code origin} and keeps exactly this contact phone — the
     * online shop's customer lookup (docs/LOJA_ONLINE_SPEC.md §3.3). Empty for a wrong phone as for an unknown
     * number, so the caller cannot tell them apart.
     */
    public Optional<OrderDTO> findByNumberAndContactPhone(Long companyId, OrderOrigin origin, String orderNumber,
                                                         String contactPhone) {
        if (contactPhone == null || contactPhone.isBlank()) {
            return Optional.empty();
        }
        return customerOrder(companyId, orderNumber)
                .filter(order -> order.getOrigin() == origin && contactPhone.equals(order.getContactPhone()))
                .map(orderLifecycleService::toDTO);
    }

    private Optional<Order> customerOrder(Long companyId, String orderNumber) {
        if (companyId == null || orderNumber == null || orderNumber.isBlank()) {
            return Optional.empty();
        }
        return orderRepository.findByCompanyIdAndOrderNumber(companyId, orderNumber.trim())
                .filter(order -> OrderKind.orDefault(order.getKind()) != OrderKind.INTERNAL_REPLENISHMENT);
    }
}

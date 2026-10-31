package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPickingSheetPrintService {

    private final OrderRepository orderRepository;
    private final OrderPickingSheetDocumentRenderer documentRenderer;

    @Autowired
    public OrderPickingSheetPrintService(
            OrderRepository orderRepository,
            OrderPickingSheetDocumentRenderer documentRenderer
    ) {
        this.orderRepository = orderRepository;
        this.documentRenderer = documentRenderer;
    }

    public OrderPickingSheetPrintService(OrderRepository orderRepository) {
        this(orderRepository, new OrderPickingSheetDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] renderA4(Long orderId, boolean reprint) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Pedido não encontrado."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        return documentRenderer.renderDocument(order, reprint);
    }
}

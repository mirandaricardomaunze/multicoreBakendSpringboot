package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPickingSheetPrintService {

    private final OrderLookup orderLookup;
    private final OrderPickingSheetDocumentRenderer documentRenderer;

    @Autowired
    public OrderPickingSheetPrintService(
            OrderLookup orderLookup,
            OrderPickingSheetDocumentRenderer documentRenderer
    ) {
        this.orderLookup = orderLookup;
        this.documentRenderer = documentRenderer;
    }

    public OrderPickingSheetPrintService(OrderLookup orderLookup) {
        this(orderLookup, new OrderPickingSheetDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] renderA4(Long orderId, boolean reprint) {
        Order order = orderLookup.findOrder(orderId)
                .orElseThrow(() -> new BusinessRuleException("Pedido não encontrado."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        return documentRenderer.renderDocument(order, reprint);
    }
}

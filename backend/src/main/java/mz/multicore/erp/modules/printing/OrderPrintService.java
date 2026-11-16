package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fachada tenant-scoped para emissão de encomendas comerciais. */
@Service
public class OrderPrintService {
    private final OrderLookup orderLookup;
    private final OrderA4Renderer a4Renderer;

    @Autowired
    public OrderPrintService(OrderLookup orderLookup, OrderA4Renderer a4Renderer) {
        this.orderLookup = orderLookup;
        this.a4Renderer = a4Renderer;
    }

    /** Preserva a construção usada antes da decomposição. */
    public OrderPrintService(OrderLookup orderLookup, LineRowMapper lineRowMapper,
                             DocumentConfigService documentConfigService) {
        this(orderLookup, new OrderA4Renderer(lineRowMapper, documentConfigService));
    }

    @Transactional(readOnly = true)
    public byte[] render(Long orderId) {
        Order order = orderLookup.requireOrder(orderId);
        CurrentUserContext.requireCompany(order.getCompany().getId());
        return a4Renderer.render(order);
    }
}

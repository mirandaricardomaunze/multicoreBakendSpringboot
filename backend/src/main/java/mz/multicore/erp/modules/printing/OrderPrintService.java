package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fachada tenant-scoped para emissão de encomendas comerciais. */
@Service
public class OrderPrintService {
    private final OrderRepository orderRepository;
    private final OrderA4Renderer a4Renderer;

    @Autowired
    public OrderPrintService(OrderRepository orderRepository, OrderA4Renderer a4Renderer) {
        this.orderRepository = orderRepository;
        this.a4Renderer = a4Renderer;
    }

    /** Preserva a construção usada antes da decomposição. */
    public OrderPrintService(OrderRepository orderRepository, LineRowMapper lineRowMapper,
                             DocumentConfigService documentConfigService) {
        this(orderRepository, new OrderA4Renderer(lineRowMapper, documentConfigService));
    }

    @Transactional(readOnly = true)
    public byte[] render(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
        CurrentUserContext.requireCompany(order.getCompany().getId());
        return a4Renderer.render(order);
    }
}

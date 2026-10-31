package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.StockTransferResolvedEvent;
import mz.multicore.erp.modules.comercial.dto.ConvertOrderToTransferRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reposição interna: a encomenda de uma loja ao armazém que a abastece, e a transferência que a
 * cumpre. Ver {@code docs/REPOSICAO_INTERNA_SPEC.md}.
 */
@Service
public class InternalReplenishmentService {

    private final ReplenishmentOrderConversionService conversionService;

    @Autowired
    public InternalReplenishmentService(ReplenishmentOrderConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public InternalReplenishmentService(OrderRepository orderRepository,
                                        StockTransferService stockTransferService,
                                        ComercialService comercialService) {
        this(new ReplenishmentOrderConversionService(orderRepository, stockTransferService, comercialService));
    }

    @Transactional
    public StockTransferDTO convertToTransfer(Long orderId, ConvertOrderToTransferRequest request) {
        return conversionService.convertToTransfer(orderId, request);
    }

    @Transactional
    public OrderDTO recordOrderFromTransfer(Long transferId) {
        return conversionService.recordOrderFromTransfer(transferId);
    }

    @EventListener
    @Transactional
    public void onTransferResolved(StockTransferResolvedEvent event) {
        conversionService.onTransferResolved(event);
    }

    static final class ReplenishmentStatus {
        static final String PENDING = "PENDING";
        static final String SEPARATED = "SEPARATED";
        static final String TRANSFER_PENDING = "TRANSFER_PENDING";
        static final String TRANSFERRED = "TRANSFERRED";

        private ReplenishmentStatus() {}
    }
}

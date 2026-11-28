package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopUnbilledOrderDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Online orders of the active company still to be billed and not cancelled (docs/LOJA_ONLINE_SPEC.md §3.5,
 * LO-UI-03), newest first: the desktop bell shows one line per order so the sales team bills it.
 */
@Service
public class OnlineShopUnbilledOrdersService {

    private final OrderLookup orderLookup;

    public OnlineShopUnbilledOrdersService(OrderLookup orderLookup) {
        this.orderLookup = orderLookup;
    }

    @Transactional(readOnly = true)
    public List<OnlineShopUnbilledOrderDTO> list() {
        return orderLookup.listUnbilledByOrigin(CurrentUserContext.requireCurrentCompanyId(), OrderOrigin.LOJA_ONLINE)
                .stream().map(OnlineShopUnbilledOrdersService::toDTO).toList();
    }

    private static OnlineShopUnbilledOrderDTO toDTO(Order order) {
        String customer = order.getWalkInName() != null && !order.getWalkInName().isBlank() ? order.getWalkInName()
                : order.getClient() == null ? null : order.getClient().getName();
        return new OnlineShopUnbilledOrderDTO(order.getId(), order.getOrderNumber(), customer,
                order.getFulfilmentLabel(), order.getCreatedAt());
    }
}

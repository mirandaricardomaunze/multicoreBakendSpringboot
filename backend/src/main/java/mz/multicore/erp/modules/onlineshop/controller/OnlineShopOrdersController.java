package mz.multicore.erp.modules.onlineshop.controller;

import mz.multicore.erp.modules.onlineshop.dto.OnlineShopUnbilledOrderDTO;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopUnbilledOrdersService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal endpoint behind the desktop bell (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-03): the sales roles read the
 * online orders still to be billed ({@code ApiRolePolicy}: SELLER and above, read only).
 */
@RestController
@RequestMapping("/api/online-shop/orders")
public class OnlineShopOrdersController {

    private final OnlineShopUnbilledOrdersService unbilledOrdersService;

    public OnlineShopOrdersController(OnlineShopUnbilledOrdersService unbilledOrdersService) {
        this.unbilledOrdersService = unbilledOrdersService;
    }

    @GetMapping("/unbilled")
    public List<OnlineShopUnbilledOrderDTO> unbilled() {
        return unbilledOrdersService.list();
    }
}

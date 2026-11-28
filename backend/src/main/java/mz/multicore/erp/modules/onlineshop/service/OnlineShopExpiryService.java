package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Automatic cancellation of online orders (docs/LOJA_ONLINE_SPEC.md §3.4, LO-10): an order of origin
 * {@link OrderOrigin#LOJA_ONLINE} still cancellable (not billed, no guide or transfer) and created more than the
 * shop's cancellation period ago is cancelled through {@link ComercialService#cancelOrder}, the same cancellation as the
 * desktop — it closes the pending approval, releases the reservation and writes the {@code ORDER_CANCEL} audit with the
 * reason. Runs as {@code SYSTEM} bound to each shop's company; a disabled shop still expires the orders it took.
 *
 * <p>Each cancellation is its own transaction, so one order refused (e.g. billed meanwhile) never blocks the others.
 */
@Service
public class OnlineShopExpiryService {

    static final String AUTO_CANCEL_REASON = "Cancelada automaticamente: encomenda da loja online não facturada em %d dia(s).";

    private static final Logger log = LoggerFactory.getLogger(OnlineShopExpiryService.class);

    private final OnlineShopSettingsRepository settingsRepository;
    private final OrderLookup orderLookup;
    private final ComercialService comercialService;

    public OnlineShopExpiryService(OnlineShopSettingsRepository settingsRepository, OrderLookup orderLookup,
                                   ComercialService comercialService) {
        this.settingsRepository = settingsRepository;
        this.orderLookup = orderLookup;
        this.comercialService = comercialService;
    }

    /** Cancels every online order past its shop's period at {@code now}; returns how many were cancelled. */
    public int cancelExpired(LocalDateTime now) {
        int cancelled = 0;
        for (OnlineShopSettings shop : settingsRepository.findAll()) {
            Long companyId = shop.getCompany().getId();
            cancelled += CurrentUserContext.runAsSystem(companyId, () -> cancelExpired(shop, companyId, now));
        }
        return cancelled;
    }

    private int cancelExpired(OnlineShopSettings shop, Long companyId, LocalDateTime now) {
        int days = shop.getAutoCancelDays();
        String reason = String.format(AUTO_CANCEL_REASON, days);
        int cancelled = 0;
        for (Order order : orderLookup.listCancellableByOriginCreatedBefore(companyId, OrderOrigin.LOJA_ONLINE,
                now.minusDays(days))) {
            try {
                comercialService.cancelOrder(order.getId(), reason);
                cancelled++;
            } catch (BusinessRuleException e) {
                log.warn("Cancelamento automático da encomenda online {} recusado: {}", order.getOrderNumber(),
                        e.getMessage());
            }
        }
        return cancelled;
    }
}

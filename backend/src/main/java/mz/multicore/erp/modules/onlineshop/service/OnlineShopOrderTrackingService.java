package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.service.CustomerOrderTrackingService;
import mz.multicore.erp.modules.onlineshop.dto.LookupOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderDTO;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The customer follows an online order without an account (docs/LOJA_ONLINE_SPEC.md §3.3, LO-09): order number plus
 * the mobile phone given at checkout, normalised the same way. Only the shop company's {@code LOJA_ONLINE} orders are
 * found; a wrong or malformed phone answers exactly like an unknown number, "Encomenda não encontrada.", so the
 * answer never reveals whether an order exists.
 */
@Service
public class OnlineShopOrderTrackingService {

    static final String ORDER_NOT_FOUND = "Encomenda não encontrada.";

    private final OnlineShopSettingsRepository settingsRepository;
    private final CustomerOrderTrackingService trackingService;

    public OnlineShopOrderTrackingService(OnlineShopSettingsRepository settingsRepository,
                                          CustomerOrderTrackingService trackingService) {
        this.settingsRepository = settingsRepository;
        this.trackingService = trackingService;
    }

    @Transactional(readOnly = true)
    public OnlineShopOrderDTO findOrder(String publicCode, LookupOnlineShopOrderRequest request) {
        OnlineShopSettings shop = OpenOnlineShops.require(settingsRepository, publicCode);
        if (request == null) {
            throw new BusinessRuleException(ORDER_NOT_FOUND);
        }
        Long companyId = shop.getCompany().getId();
        return OnlineShopCheckoutRules.normalisedMobilePhone(request.phone())
                .flatMap(phone -> trackingService.findByNumberAndContactPhone(companyId, OrderOrigin.LOJA_ONLINE,
                        request.number(), phone))
                .map(OnlineShopOrderService::toDTO)
                .orElseThrow(() -> new BusinessRuleException(ORDER_NOT_FOUND));
    }
}

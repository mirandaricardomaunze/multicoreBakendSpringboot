package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;

import java.util.Locale;

/**
 * The shop behind {@code /loja/{code}} (docs/LOJA_ONLINE_SPEC.md §3.1): the code ignores case and surrounding spaces;
 * a disabled or unknown shop answers "Loja indisponível." to the catalogue and to the checkout alike.
 */
final class OpenOnlineShops {

    static final String SHOP_UNAVAILABLE = "Loja indisponível.";

    private OpenOnlineShops() {
    }

    static OnlineShopSettings require(OnlineShopSettingsRepository repository, String publicCode) {
        if (publicCode == null || publicCode.isBlank()) {
            throw new BusinessRuleException(SHOP_UNAVAILABLE);
        }
        return repository.findByPublicCode(publicCode.trim().toLowerCase(Locale.ROOT))
                .filter(OnlineShopSettings::isEnabled)
                .orElseThrow(() -> new BusinessRuleException(SHOP_UNAVAILABLE));
    }
}

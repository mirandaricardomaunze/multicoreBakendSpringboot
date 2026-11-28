package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseService;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopPickupStoreDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopStorefrontDTO;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Header and checkout options of the public page {@code /loja/{code}} (docs/LOJA_ONLINE_SPEC.md §3.2–3.3, LO-11): the
 * company's display name, contact phone, pickup stores (in the ADMIN's order, only those still sales warehouses of
 * the company) and delivery zones with their fee. A disabled or unknown shop answers "Loja indisponível.".
 */
@Service
public class OnlineShopStorefrontService {

    private final OnlineShopSettingsRepository settingsRepository;
    private final WarehouseService warehouseService;

    public OnlineShopStorefrontService(OnlineShopSettingsRepository settingsRepository,
                                       WarehouseService warehouseService) {
        this.settingsRepository = settingsRepository;
        this.warehouseService = warehouseService;
    }

    @Transactional(readOnly = true)
    public OnlineShopStorefrontDTO storefront(String publicCode) {
        OnlineShopSettings shop = OpenOnlineShops.require(settingsRepository, publicCode);
        Long companyId = shop.getCompany().getId();
        // Página pública: sem utilizador; lê os armazéns como a própria loja, na empresa da loja (como o checkout).
        Map<Long, Warehouse> sales = CurrentUserContext.runAs(OnlineShopOrderService.actorName(shop),
                        OnlineShopOrderService.ACTOR_ROLE, companyId,
                        () -> warehouseService.getSalesWarehousesByCompany(companyId)).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        List<OnlineShopPickupStoreDTO> pickups = shop.getPickupWarehouseIds().stream()
                .map(sales::get)
                .filter(Objects::nonNull)
                .map(w -> new OnlineShopPickupStoreDTO(w.getId(), w.getName()))
                .toList();
        List<OnlineShopDeliveryZoneDTO> zones = shop.getDeliveryZones().stream()
                .map(zone -> new OnlineShopDeliveryZoneDTO(zone.getName(), zone.getFee()))
                .toList();
        return new OnlineShopStorefrontDTO(shop.getCompany().displayName(), shop.getContactPhone(), pickups, zones);
    }
}

package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HTTP client for the online shop back office of the active company ({@code /api/online-shop},
 * docs/LOJA_ONLINE_SPEC.md §3.5): the ADMIN reads and saves the settings (LO-UI-01). The pickup store candidates are
 * the company's sales warehouses, read through the inventory client.
 */
@Component
@Profile("desktop")
public class OnlineShopApiClient {

    public static final String SETTINGS = "/api/online-shop/settings";

    private final DesktopClientFactory clientFactory;
    private final InventoryApiClient inventoryApiClient;

    public OnlineShopApiClient(DesktopClientFactory clientFactory, InventoryApiClient inventoryApiClient) {
        this.clientFactory = clientFactory;
        this.inventoryApiClient = inventoryApiClient;
    }

    public OnlineShopSettingsDTO settings() {
        return clientFactory.authenticatedClient().get(SETTINGS, OnlineShopSettingsDTO.class);
    }

    public OnlineShopSettingsDTO saveSettings(SaveOnlineShopSettingsRequest request) {
        return clientFactory.authenticatedClient().put(SETTINGS, request, OnlineShopSettingsDTO.class);
    }

    /** The sales warehouses of the active company: one "Lojas de levantamento" box each. */
    public List<WarehouseDTO> salesWarehouses() {
        return inventoryApiClient.getSalesWarehousesByCompany(CurrentUserContext.getCurrentCompanyId());
    }
}

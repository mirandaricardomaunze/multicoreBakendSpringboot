package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopPublicationDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopUnbilledOrderDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import mz.multicore.erp.modules.onlineshop.dto.SetOnlineShopPublicationRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HTTP client for the online shop back office of the active company ({@code /api/online-shop},
 * docs/LOJA_ONLINE_SPEC.md §3.5): the ADMIN reads and saves the settings (LO-UI-01). The pickup store candidates are
 * the company's sales warehouses, read through the inventory client. The product form reads and switches
 * "Publicar na loja online" of a product (LO-UI-02). The bell reads the online orders still to be billed (LO-UI-03).
 */
@Component
@Profile("desktop")
public class OnlineShopApiClient {

    public static final String SETTINGS = "/api/online-shop/settings";
    public static final String PRODUCTS = "/api/online-shop/products/";
    public static final String UNBILLED_ORDERS = "/api/online-shop/orders/unbilled";

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

    /** Whether the product is published on the online shop. */
    public boolean isPublished(Long productId) {
        OnlineShopPublicationDTO publication = clientFactory.authenticatedClient()
                .get(publicationPath(productId), OnlineShopPublicationDTO.class);
        return publication != null && publication.published();
    }

    /** Publishes or withdraws the product; the backend audits it and keeps its refusals. */
    public void setPublished(Long productId, boolean published) {
        clientFactory.authenticatedClient().put(publicationPath(productId),
                new SetOnlineShopPublicationRequest(published), OnlineShopPublicationDTO.class);
    }

    private static String publicationPath(Long productId) {
        return PRODUCTS + productId + "/published";
    }

    /** Online orders of the active company not billed nor cancelled, newest first (bell, LO-UI-03). */
    public List<OnlineShopUnbilledOrderDTO> unbilledOrders() {
        return clientFactory.authenticatedClient().getList(UNBILLED_ORDERS, OnlineShopUnbilledOrderDTO.class);
    }

    /** The sales warehouses of the active company: one "Lojas de levantamento" box each. */
    public List<WarehouseDTO> salesWarehouses() {
        return inventoryApiClient.getSalesWarehousesByCompany(CurrentUserContext.getCurrentCompanyId());
    }
}

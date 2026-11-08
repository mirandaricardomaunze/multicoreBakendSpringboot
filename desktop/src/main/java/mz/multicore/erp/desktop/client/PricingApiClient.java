package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.pricing.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** HTTP client for price lists ({@code /api/pricing}, SA-C03). */
@Component
@Profile("desktop")
public class PricingApiClient {

    private final DesktopClientFactory clientFactory;

    public PricingApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<PriceListDTO> lists(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/pricing/lists?companyId=" + companyId, PriceListDTO.class);
    }

    public PriceListDTO save(SavePriceListRequest request) {
        return clientFactory.authenticatedClient().put("/api/pricing/lists", request, PriceListDTO.class);
    }

    public void delete(Long companyId, Long priceListId) {
        clientFactory.authenticatedClient().delete("/api/pricing/lists/" + priceListId + "?companyId=" + companyId);
    }

    public List<PriceListItemDTO> items(Long companyId, Long priceListId) {
        return clientFactory.authenticatedClient().getList(
                "/api/pricing/lists/" + priceListId + "/items?companyId=" + companyId, PriceListItemDTO.class);
    }

    public PriceListItemDTO saveItem(SavePriceListItemRequest request) {
        return clientFactory.authenticatedClient().put("/api/pricing/items", request, PriceListItemDTO.class);
    }

    public void deleteItem(Long companyId, Long itemId) {
        clientFactory.authenticatedClient().delete("/api/pricing/items/" + itemId + "?companyId=" + companyId);
    }

    public List<ClientPriceListDTO> clients(Long companyId, Long priceListId) {
        return clientFactory.authenticatedClient().getList(
                "/api/pricing/lists/" + priceListId + "/clients?companyId=" + companyId, ClientPriceListDTO.class);
    }

    public ClientPriceListDTO clientAssignment(Long companyId, Long clientId) {
        return clientFactory.authenticatedClient().get("/api/pricing/clients/" + clientId + "?companyId=" + companyId,
                ClientPriceListDTO.class);
    }

    public ClientPriceListDTO assign(Long companyId, Long clientId, Long priceListId) {
        return clientFactory.authenticatedClient().put("/api/pricing/clients",
                new AssignClientPriceListRequest(companyId, clientId, priceListId), ClientPriceListDTO.class);
    }

    public ResolvedPriceDTO resolve(Long companyId, Long clientId, Long productId, BigDecimal quantity) {
        return clientFactory.authenticatedClient().get("/api/pricing/resolve?companyId=" + companyId
                + (clientId == null ? "" : "&clientId=" + clientId) + "&productId=" + productId
                + "&quantity=" + quantity.toPlainString(), ResolvedPriceDTO.class);
    }
}

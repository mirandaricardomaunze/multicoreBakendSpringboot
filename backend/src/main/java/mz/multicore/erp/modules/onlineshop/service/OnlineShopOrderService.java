package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.ExternalOrderSource;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.ExternalOrderPlacementService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopFulfilment;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineDTO;
import mz.multicore.erp.modules.onlineshop.dto.PlaceOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout of the public online shop (docs/LOJA_ONLINE_SPEC.md §3.3, LO-06): the validated cart becomes a customer
 * order through the same placement service as the public API ({@link ExternalOrderPlacementService}, API §3.4) — a
 * formal order in approval that reserves free stock, with prices and VAT from the system, the client by NUIT
 * (registered with the customer's name when new) or Consumidor Final, the contact phone kept on the order and origin
 * {@link OrderOrigin#LOJA_ONLINE}. The shop acts as a seller of its company, like an API key.
 */
@Service
public class OnlineShopOrderService {

    /** Name shown as the origin of the order and in the audit trail. */
    static final String ORIGIN_NAME = "Loja online";
    /** Same approval thresholds as an order typed at the desktop by a SELLER (API §3.4.1). */
    static final String ACTOR_ROLE = "SELLER";

    private final OnlineShopSettingsRepository settingsRepository;
    private final ProductLookup productLookup;
    private final ClientLookup clientLookup;
    private final ComercialService comercialService;
    private final ExternalOrderPlacementService placementService;

    public OnlineShopOrderService(OnlineShopSettingsRepository settingsRepository, ProductLookup productLookup,
                                  ClientLookup clientLookup, ComercialService comercialService,
                                  ExternalOrderPlacementService placementService) {
        this.settingsRepository = settingsRepository;
        this.productLookup = productLookup;
        this.clientLookup = clientLookup;
        this.comercialService = comercialService;
        this.placementService = placementService;
    }

    /** Places the order of the shop behind {@code /loja/{code}}; nothing is saved when a rule refuses it. */
    @Transactional
    public OnlineShopOrderDTO placeOrder(String publicCode, PlaceOnlineShopOrderRequest request) {
        OnlineShopSettings shop = OpenOnlineShops.require(settingsRepository, publicCode);
        OnlineShopCheckout checkout = OnlineShopCheckoutRules.validate(request, shop);
        Long companyId = shop.getCompany().getId();
        return CurrentUserContext.runAs(actorName(shop), ACTOR_ROLE, companyId,
                () -> toDTO(place(companyId, shop, checkout)));
    }

    /** Audit name of the shop acting on the order, e.g. {@code Loja online · mercearia-baixa}. */
    static String actorName(OnlineShopSettings shop) {
        return ORIGIN_NAME + " · " + shop.getPublicCode();
    }

    private OrderDTO place(Long companyId, OnlineShopSettings shop, OnlineShopCheckout checkout) {
        List<CreateInvoiceLineRequest> lines = checkout.lines().stream().map(line -> {
            Product product = requireOnSale(companyId, line.sku());
            return new CreateInvoiceLineRequest(product.getId(), line.quantity(), product.effectiveTaxRate(),
                    BigDecimal.ZERO, null, null);
        }).toList();
        boolean walkIn = checkout.nuit() == null;
        Long clientId = walkIn ? null : customerFor(companyId, checkout);
        CreateOrderRequest order = new CreateOrderRequest(clientId, walkIn ? checkout.customerName() : null,
                companyId, warehouseFor(shop, checkout), lines, OrderKind.FORMAL_ORDER, null, null, null, null,
                checkout.notes());
        return placementService.placeReservedFormalOrder(order,
                new ExternalOrderSource(OrderOrigin.LOJA_ONLINE, ORIGIN_NAME, null, null, null, checkout.phone()));
    }

    /** Only products the shop shows can be ordered; the price is never taken from the request. */
    private Product requireOnSale(Long companyId, String sku) {
        return productLookup.findBySkuForCompany(sku, companyId)
                .filter(OnlineShopCatalogService::isOnSale)
                .orElseThrow(() -> new BusinessRuleException("O artigo " + sku + " não está à venda na loja online."));
    }

    /** The pickup store; for home delivery the first pickup store until LO-07 picks the one with stock. */
    private static Long warehouseFor(OnlineShopSettings shop, OnlineShopCheckout checkout) {
        return checkout.fulfilment() == OnlineShopFulfilment.LEVANTAMENTO
                ? checkout.pickupWarehouseId()
                : shop.getPickupWarehouseIds().get(0);
    }

    /** The company's client with this NUIT, registered with the customer's name by the manual rules when new. */
    private Long customerFor(Long companyId, OnlineShopCheckout checkout) {
        return clientLookup.findForCompanyByTaxId(checkout.nuit(), companyId).map(Client::getId)
                .orElseGet(() -> comercialService.createClient(
                        new SaveClientRequest(checkout.customerName(), checkout.nuit(), null, null)).id());
    }

    private static OnlineShopOrderDTO toDTO(OrderDTO order) {
        List<OnlineShopOrderLineDTO> lines = order.lines() == null ? List.of() : order.lines().stream()
                .map(line -> new OnlineShopOrderLineDTO(line.sku(), line.productName(), line.quantity()))
                .toList();
        return new OnlineShopOrderDTO(order.orderNumber(), order.statusLabel(), lines, order.totalAmount());
    }
}

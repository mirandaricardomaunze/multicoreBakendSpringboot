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
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopFulfilment;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineDTO;
import mz.multicore.erp.modules.onlineshop.dto.PlaceOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checkout of the public online shop (docs/LOJA_ONLINE_SPEC.md §3.3, LO-06): the validated cart becomes a customer
 * order through the same placement service as the public API ({@link ExternalOrderPlacementService}, API §3.4) — a
 * formal order in approval that reserves free stock, with prices and VAT from the system, the client by NUIT
 * (registered with the customer's name when new) or Consumidor Final, the contact phone kept on the order and origin
 * {@link OrderOrigin#LOJA_ONLINE}. The shop acts as a seller of its company, like an API key.
 *
 * <p>LO-07 (§3.3): the order's store is the chosen pickup store; for home delivery, the first pickup store (in the
 * shop's order) with free stock for the whole cart, otherwise "Não há stock suficiente para entrega". A pickup store
 * short of any product refuses the order with the list of those products. Nothing is created when refused.
 *
 * <p>LO-08 (§3.3): home delivery adds one service line "Taxa de entrega — {zona}" at the zone fee and the standard
 * VAT ({@link DeliveryFeeProductProvisioner}); pickup is free.
 */
@Service
public class OnlineShopOrderService {

    /** Name shown as the origin of the order and in the audit trail. */
    static final String ORIGIN_NAME = "Loja online";
    /** Same approval thresholds as an order typed at the desktop by a SELLER (API §3.4.1). */
    static final String ACTOR_ROLE = "SELLER";
    static final String NO_STOCK_FOR_DELIVERY = "Não há stock suficiente para entrega.";

    private final OnlineShopSettingsRepository settingsRepository;
    private final ProductLookup productLookup;
    private final ClientLookup clientLookup;
    private final ComercialService comercialService;
    private final ExternalOrderPlacementService placementService;
    private final StockLookup stockLookup;
    private final OrderReservationLookup reservationLookup;
    private final DeliveryFeeProductProvisioner deliveryFeeProducts;

    public OnlineShopOrderService(OnlineShopSettingsRepository settingsRepository, ProductLookup productLookup,
                                  ClientLookup clientLookup, ComercialService comercialService,
                                  ExternalOrderPlacementService placementService, StockLookup stockLookup,
                                  OrderReservationLookup reservationLookup,
                                  DeliveryFeeProductProvisioner deliveryFeeProducts) {
        this.settingsRepository = settingsRepository;
        this.productLookup = productLookup;
        this.clientLookup = clientLookup;
        this.comercialService = comercialService;
        this.placementService = placementService;
        this.stockLookup = stockLookup;
        this.reservationLookup = reservationLookup;
        this.deliveryFeeProducts = deliveryFeeProducts;
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
        List<CartItem> cart = checkout.lines().stream()
                .map(line -> new CartItem(requireOnSale(companyId, line.sku()), line.quantity()))
                .toList();
        Long warehouseId = warehouseFor(companyId, shop, checkout, cart);
        List<CreateInvoiceLineRequest> lines = new ArrayList<>(cart.stream()
                .map(item -> line(item.product(), item.quantity()))
                .toList());
        if (checkout.fulfilment() == OnlineShopFulfilment.ENTREGA) {
            lines.add(line(deliveryFeeProducts.forZone(shop.getCompany(), checkout.deliveryZone()), BigDecimal.ONE));
        }
        boolean walkIn = checkout.nuit() == null;
        Long clientId = walkIn ? null : customerFor(companyId, checkout);
        CreateOrderRequest order = new CreateOrderRequest(clientId, walkIn ? checkout.customerName() : null,
                companyId, warehouseId, lines, OrderKind.FORMAL_ORDER, null, null, null, null, checkout.notes());
        return placementService.placeReservedFormalOrder(order,
                new ExternalOrderSource(OrderOrigin.LOJA_ONLINE, ORIGIN_NAME, null, null, null, checkout.phone()));
    }

    /**
     * The chosen pickup store when it has free stock for the whole cart; for home delivery the first pickup store
     * that has it. Refused otherwise, before anything is created.
     */
    private Long warehouseFor(Long companyId, OnlineShopSettings shop, OnlineShopCheckout checkout,
                              List<CartItem> cart) {
        PickupFreeStock freeStock = PickupFreeStock.load(companyId, shop.getPickupWarehouseIds(), stockLookup,
                reservationLookup);
        if (checkout.fulfilment() == OnlineShopFulfilment.LEVANTAMENTO) {
            List<String> missing = shortOf(freeStock, checkout.pickupWarehouseId(), cart);
            if (!missing.isEmpty()) {
                throw new BusinessRuleException("Sem stock suficiente na loja escolhida para: "
                        + String.join(", ", missing) + ".");
            }
            return checkout.pickupWarehouseId();
        }
        return shop.getPickupWarehouseIds().stream()
                .filter(warehouseId -> shortOf(freeStock, warehouseId, cart).isEmpty())
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(NO_STOCK_FOR_DELIVERY));
    }

    /** Order line at the product's system price and VAT; the price never comes from the request. */
    private static CreateInvoiceLineRequest line(Product product, BigDecimal quantity) {
        return new CreateInvoiceLineRequest(product.getId(), quantity, product.effectiveTaxRate(), BigDecimal.ZERO,
                null, null);
    }

    /** Names of the stock-tracked products whose free stock in the store does not cover the cart quantity. */
    private static List<String> shortOf(PickupFreeStock freeStock, Long warehouseId, List<CartItem> cart) {
        Map<Long, BigDecimal> needed = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (CartItem item : cart) {
            if (item.product().isStockTracked()) {
                needed.merge(item.product().getId(), item.quantity(), BigDecimal::add);
                names.putIfAbsent(item.product().getId(), item.product().getName());
            }
        }
        return needed.entrySet().stream()
                .filter(e -> freeStock.free(e.getKey(), warehouseId).compareTo(e.getValue()) < 0)
                .map(e -> names.get(e.getKey()))
                .toList();
    }

    /** A cart line with the system's product, resolved once per checkout. */
    private record CartItem(Product product, BigDecimal quantity) {
    }

    /** Only products the shop shows can be ordered; the price is never taken from the request. */
    private Product requireOnSale(Long companyId, String sku) {
        return productLookup.findBySkuForCompany(sku, companyId)
                .filter(OnlineShopCatalogService::isOnSale)
                .orElseThrow(() -> new BusinessRuleException("O artigo " + sku + " não está à venda na loja online."));
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

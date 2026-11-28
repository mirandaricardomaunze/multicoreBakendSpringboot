package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopProductDTO;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalogue of the public online shop (docs/LOJA_ONLINE_SPEC.md §3.1, LO-02): only the company's active products
 * with "Publicar na loja online" switched on (never the system advance product), and the switch itself. Products are
 * read and written through the comercial module's lookup and command services.
 *
 * <p>LO-03 (§3.2): a product is Disponível when stock minus active order reservations is positive in at least one
 * pickup shop of the company (services without stock control always are), otherwise Esgotado. Quantities are only
 * used here and never leave the service.
 */
@Service
public class OnlineShopCatalogService {

    private final OnlineShopSettingsRepository settingsRepository;
    private final ProductLookup productLookup;
    private final ProductCommandService productCommandService;
    private final AuditLogService auditLogService;
    private final StockLookup stockLookup;
    private final OrderReservationLookup reservationLookup;

    public OnlineShopCatalogService(OnlineShopSettingsRepository settingsRepository, ProductLookup productLookup,
                                    ProductCommandService productCommandService, AuditLogService auditLogService,
                                    StockLookup stockLookup, OrderReservationLookup reservationLookup) {
        this.settingsRepository = settingsRepository;
        this.productLookup = productLookup;
        this.productCommandService = productCommandService;
        this.auditLogService = auditLogService;
        this.stockLookup = stockLookup;
        this.reservationLookup = reservationLookup;
    }

    /** Published products of the shop behind {@code /loja/{code}}; disabled or unknown shop → "Loja indisponível.". */
    @Transactional(readOnly = true)
    public List<OnlineShopProductDTO> listProducts(String publicCode) {
        OnlineShopSettings shop = OpenOnlineShops.require(settingsRepository, publicCode);
        Long companyId = shop.getCompany().getId();
        Set<Long> inStock = productsWithFreeStock(companyId, Set.copyOf(shop.getPickupWarehouseIds()));
        return productLookup.listForCompany(companyId).stream()
                .filter(OnlineShopCatalogService::isOnSale)
                .map(p -> toDTO(p, !p.isStockTracked() || inStock.contains(p.getId())))
                .toList();
    }

    /** Switches "Publicar na loja online" of a product of the active company, audited. */
    @Transactional
    public void setPublished(Long productId, boolean published) {
        Product product = productLookup.requireForCompany(productId, CurrentUserContext.requireCurrentCompanyId());
        if (published) {
            product.requireOrdinarySale();
        }
        product.setPublishedOnline(published);
        productCommandService.saveProduct(product);
        auditLogService.logCurrent("ONLINE_SHOP_PUBLISH", "Artigo " + product.getSku()
                + (published ? " publicado na" : " retirado da") + " loja online");
    }

    /** Products with stock minus active reservations above zero in at least one of the pickup shops. */
    private Set<Long> productsWithFreeStock(Long companyId, Set<Long> pickupWarehouseIds) {
        Map<String, BigDecimal> reserved = new HashMap<>();
        for (ReservedStock r : reservationLookup.activeReservationsForCompany(companyId)) {
            if (r.quantity() != null) {
                reserved.merge(key(r.productId(), r.warehouseId()), r.quantity(), BigDecimal::add);
            }
        }
        Set<Long> inStock = new HashSet<>();
        for (Stock stock : stockLookup.listForCompany(companyId)) {
            if (stock.getProduct() == null || stock.getWarehouse() == null || stock.getQuantity() == null
                    || !pickupWarehouseIds.contains(stock.getWarehouse().getId())) {
                continue;
            }
            Long productId = stock.getProduct().getId();
            BigDecimal free = stock.getQuantity()
                    .subtract(reserved.getOrDefault(key(productId, stock.getWarehouse().getId()), BigDecimal.ZERO));
            if (free.signum() > 0) {
                inStock.add(productId);
            }
        }
        return inStock;
    }

    private static String key(Long productId, Long warehouseId) {
        return productId + ":" + warehouseId;
    }

    /** Published and not the system advance product: the only products the shop shows and sells. */
    static boolean isOnSale(Product product) {
        return product.isPublishedOnline() && !AdvancePayments.isSystemSku(product.getSku());
    }

    private static OnlineShopProductDTO toDTO(Product product, boolean available) {
        BigDecimal priceWithVat = LineCalculator.compute(product.getUnitPrice(), BigDecimal.ONE, BigDecimal.ZERO,
                product.effectiveTaxRate()).total();
        return new OnlineShopProductDTO(product.getSku(), product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : null, priceWithVat,
                available);
    }
}

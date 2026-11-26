package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.publicapi.dto.PublicPage;
import mz.multicore.erp.modules.publicapi.dto.PublicProductDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicStockDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Catalogue and available stock of the public API (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3, §3.3.1, API-07), always
 * for the company of the calling key. Reads through the owning modules' lookups; costs never reach the DTOs.
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogService {

    private static final Comparator<PublicStockDTO> STOCK_ORDER = Comparator.comparing(PublicStockDTO::sku)
            .thenComparing(PublicStockDTO::warehouseName, String.CASE_INSENSITIVE_ORDER);

    private final ProductLookup productLookup;
    private final WarehouseLookup warehouseLookup;
    private final StockLookup stockLookup;
    private final OrderReservationLookup reservationLookup;

    public PublicCatalogService(ProductLookup productLookup, WarehouseLookup warehouseLookup,
                                StockLookup stockLookup, OrderReservationLookup reservationLookup) {
        this.productLookup = productLookup;
        this.warehouseLookup = warehouseLookup;
        this.stockLookup = stockLookup;
        this.reservationLookup = reservationLookup;
    }

    public PublicPage<PublicProductDTO> listProducts(Long companyId, String updatedSince, Integer page, Integer size) {
        LocalDateTime since = PublicApiQuery.instant("updatedSince", updatedSince);
        List<PublicProductDTO> products = productLookup.listForCompany(companyId).stream()
                .filter(PublicCatalogService::isPublic)
                .filter(p -> since == null || changedSince(p, since))
                .map(PublicCatalogMapper::toProduct)
                .sorted(Comparator.comparing(PublicProductDTO::sku))
                .toList();
        return PublicApiQuery.page(products, page, size);
    }

    public PublicProductDTO getProduct(Long companyId, String sku) {
        return productLookup.findBySkuForCompany(sku, companyId)
                .filter(PublicCatalogService::isPublic)
                .map(PublicCatalogMapper::toProduct)
                .orElseThrow(() -> new PublicApiNotFoundException("PRODUCT_NOT_FOUND",
                        "Artigo com o SKU " + sku + " não encontrado."));
    }

    public PublicPage<PublicStockDTO> listStock(Long companyId, String warehouse, Integer page, Integer size) {
        List<Warehouse> salesWarehouses = warehouseLookup.listForCompany(companyId).stream()
                .filter(PublicCatalogService::isSalesWarehouse)
                .filter(byCodeOrName(warehouse))
                .toList();
        if (warehouse != null && !warehouse.isBlank() && salesWarehouses.isEmpty()) {
            throw new PublicApiNotFoundException("WAREHOUSE_NOT_FOUND",
                    "Armazém de venda " + warehouse.trim() + " não encontrado.");
        }
        Set<Long> warehouseIds = salesWarehouses.stream().map(Warehouse::getId).collect(Collectors.toSet());
        Map<String, BigDecimal> reserved = reservedByProductAndWarehouse(companyId);
        List<PublicStockDTO> rows = stockLookup.listForCompany(companyId).stream()
                .filter(s -> s.getProduct() != null && s.getWarehouse() != null)
                .filter(s -> warehouseIds.contains(s.getWarehouse().getId()))
                .filter(s -> s.getProduct().isStockTracked() && isPublic(s.getProduct()))
                .map(s -> PublicCatalogMapper.toStock(s, reserved.getOrDefault(key(s), BigDecimal.ZERO)))
                .sorted(STOCK_ORDER)
                .toList();
        return PublicApiQuery.page(rows, page, size);
    }

    private Map<String, BigDecimal> reservedByProductAndWarehouse(Long companyId) {
        Map<String, BigDecimal> reserved = new HashMap<>();
        for (ReservedStock r : reservationLookup.activeReservationsForCompany(companyId)) {
            reserved.merge(key(r.productId(), r.warehouseId()),
                    r.quantity() == null ? BigDecimal.ZERO : r.quantity(), BigDecimal::add);
        }
        return reserved;
    }

    private static String key(Stock stock) {
        return key(stock.getProduct().getId(), stock.getWarehouse().getId());
    }

    private static String key(Long productId, Long warehouseId) {
        return productId + ":" + warehouseId;
    }

    /** Active = associated with the company (the only state the register has); never the system advance product. */
    private static boolean isPublic(Product product) {
        return !AdvancePayments.isSystemSku(product.getSku());
    }

    private static boolean changedSince(Product product, LocalDateTime since) {
        LocalDateTime changed = PublicCatalogMapper.lastChange(product);
        return changed != null && !changed.isBefore(since);
    }

    private static boolean isSalesWarehouse(Warehouse warehouse) {
        return warehouse.isActive() && warehouse.isAllowsSales() && warehouse.getType() != WarehouseType.TRANSIT;
    }

    private static Predicate<Warehouse> byCodeOrName(String warehouse) {
        if (warehouse == null || warehouse.isBlank()) {
            return w -> true;
        }
        String wanted = warehouse.trim().toLowerCase(Locale.ROOT);
        return w -> wanted.equals(lower(w.getWarehouseNumber())) || wanted.equals(lower(w.getName()));
    }

    private static String lower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}

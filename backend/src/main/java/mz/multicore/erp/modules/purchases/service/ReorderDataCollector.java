package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.repository.PurchaseLineRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReorderDataCollector {

    public record ProductPurchaseInfo(Long supplierId, String supplierName, BigDecimal unitPrice) {}

    private final StockRepository stockRepository;
    private final InvoiceLookup invoiceLookup;
    private final PurchaseLineRepository purchaseLineRepository;

    public ReorderDataCollector(StockRepository stockRepository,
                                InvoiceLookup invoiceLookup,
                                PurchaseLineRepository purchaseLineRepository) {
        this.stockRepository = stockRepository;
        this.invoiceLookup = invoiceLookup;
        this.purchaseLineRepository = purchaseLineRepository;
    }

    public Map<Long, BigDecimal> collectStockByProduct(Long companyId) {
        Map<Long, BigDecimal> stockByProduct = new HashMap<>();
        for (Stock s : stockRepository.findByWarehouseCompanyId(companyId)) {
            Long pid = s.getProduct().getId();
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            stockByProduct.merge(pid, qty, BigDecimal::add);
        }
        return stockByProduct;
    }

    public Map<Long, BigDecimal> collectSalesLast30Days(Long companyId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Map<Long, BigDecimal> salesLast30Days = new HashMap<>();
        List<Object[]> salesAgg = invoiceLookup.sumQuantitySoldByProductSince(
                companyId, InvoiceStatus.realisedSaleStatuses(), thirtyDaysAgo);
        if (salesAgg != null) {
            for (Object[] row : salesAgg) {
                if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
                Long pid = row[0] instanceof Long l ? l : ((Number) row[0]).longValue();
                BigDecimal totalQty = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
                salesLast30Days.put(pid, totalQty);
            }
        }
        return salesLast30Days;
    }

    public Map<Long, ProductPurchaseInfo> collectRecentPurchases(Long companyId) {
        Map<Long, ProductPurchaseInfo> recentPurchaseByProduct = new HashMap<>();
        List<PurchaseLine> recentPurchases = purchaseLineRepository.findRecentPurchasesByCompany(companyId);
        if (recentPurchases != null) {
            for (PurchaseLine pl : recentPurchases) {
                if (pl == null || pl.getProduct() == null) continue;
                Long pid = pl.getProduct().getId();
                if (!recentPurchaseByProduct.containsKey(pid)) {
                    Long supId = (pl.getPurchase() != null && pl.getPurchase().getSupplier() != null)
                            ? pl.getPurchase().getSupplier().getId() : null;
                    String supName = (pl.getPurchase() != null && pl.getPurchase().getSupplier() != null)
                            ? pl.getPurchase().getSupplier().getName() : null;
                    BigDecimal price = pl.getUnitPrice() != null ? pl.getUnitPrice() : BigDecimal.ZERO;
                    recentPurchaseByProduct.put(pid, new ProductPurchaseInfo(supId, supName, price));
                }
            }
        }
        return recentPurchaseByProduct;
    }
}

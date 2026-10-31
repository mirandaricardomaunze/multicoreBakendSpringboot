package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;
import mz.multicore.erp.modules.purchases.service.ReorderDataCollector.ProductPurchaseInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ReorderSuggestionCalculator {

    private static final BigDecimal DAYS_WINDOW = new BigDecimal("30");

    public List<ReorderSuggestionDTO> computeSuggestions(
            List<Product> products,
            Map<Long, BigDecimal> stockByProduct,
            Map<Long, BigDecimal> salesLast30Days,
            Map<Long, ProductPurchaseInfo> recentPurchaseByProduct) {
        List<ReorderSuggestionDTO> out = new ArrayList<>();
        for (Product p : products) {
            if (!p.isStockTracked()) continue;
            BigDecimal min = p.getMinStock();
            if (min == null || min.signum() <= 0) continue;

            BigDecimal current = stockByProduct.getOrDefault(p.getId(), BigDecimal.ZERO);
            if (current.compareTo(min) >= 0) continue;

            int upb = p.getUnitsPerBox() <= 0 ? 1 : p.getUnitsPerBox();
            BigDecimal deficit = min.subtract(current);
            BigDecimal boxes = deficit.divide(BigDecimal.valueOf(upb), 0, RoundingMode.CEILING);
            BigDecimal suggestedUnits = boxes.multiply(BigDecimal.valueOf(upb));

            BigDecimal totalSold30 = salesLast30Days.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal dailySalesRate = totalSold30.divide(DAYS_WINDOW, 2, RoundingMode.HALF_UP);

            Integer daysRemaining = null;
            if (current.signum() <= 0) {
                daysRemaining = 0;
            } else if (dailySalesRate.signum() > 0) {
                daysRemaining = current.divide(dailySalesRate, 0, RoundingMode.FLOOR).intValue();
            }

            String urgencyStatus;
            if (current.signum() <= 0) {
                urgencyStatus = "ESGOTADO";
            } else if (daysRemaining != null && daysRemaining <= 7) {
                urgencyStatus = "CRÍTICO";
            } else {
                urgencyStatus = "BAIXO";
            }

            ProductPurchaseInfo lastPurchase = recentPurchaseByProduct.get(p.getId());
            Long supplierId = null;
            String supplierName = null;
            BigDecimal estPrice = p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO;

            if (lastPurchase != null) {
                supplierId = lastPurchase.supplierId();
                supplierName = lastPurchase.supplierName();
                if (lastPurchase.unitPrice() != null && lastPurchase.unitPrice().signum() > 0) {
                    estPrice = lastPurchase.unitPrice();
                }
            }

            BigDecimal estTotalCost = suggestedUnits.multiply(estPrice).setScale(2, RoundingMode.HALF_UP);

            out.add(new ReorderSuggestionDTO(
                    p.getId(), p.getSku(), p.getName(), current, min, upb, boxes, suggestedUnits,
                    dailySalesRate, daysRemaining, urgencyStatus, supplierId, supplierName,
                    estPrice, estTotalCost,
                    p.getBusinessUnit() != null ? p.getBusinessUnit().getId() : null,
                    p.getBusinessUnit() != null ? p.getBusinessUnit().getName() : null
            ));
        }

        out.sort((a, b) -> {
            int rankA = urgencyRank(a.urgencyStatus());
            int rankB = urgencyRank(b.urgencyStatus());
            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }
            if (a.daysRemaining() != null && b.daysRemaining() != null) {
                int cmp = Integer.compare(a.daysRemaining(), b.daysRemaining());
                if (cmp != 0) return cmp;
            } else if (a.daysRemaining() != null) {
                return -1;
            } else if (b.daysRemaining() != null) {
                return 1;
            }
            BigDecimal covA = a.currentStock().divide(a.minStock(), 4, RoundingMode.HALF_UP);
            BigDecimal covB = b.currentStock().divide(b.minStock(), 4, RoundingMode.HALF_UP);
            return covA.compareTo(covB);
        });

        return out;
    }

    private static int urgencyRank(String status) {
        if ("ESGOTADO".equalsIgnoreCase(status)) return 1;
        if ("CRÍTICO".equalsIgnoreCase(status)) return 2;
        return 3;
    }
}

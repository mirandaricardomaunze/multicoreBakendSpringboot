package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.repository.PurchaseLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reposição automática e inteligente de stock:
 * Calcula a velocidade diária de vendas (últimos 30 dias), previsão de dias até à rutura,
 * status de criticidade (ESGOTADO, CRÍTICO, BAIXO), fornecedor habitual e custo estimado
 * arredondado a caixas inteiras.
 */
@Service
public class ReorderService {

    private static final BigDecimal DAYS_WINDOW = new BigDecimal("30");

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final PurchaseLineRepository purchaseLineRepository;

    public ReorderService(
            StockRepository stockRepository,
            ProductRepository productRepository,
            InvoiceRepository invoiceRepository,
            PurchaseLineRepository purchaseLineRepository
    ) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.invoiceRepository = invoiceRepository;
        this.purchaseLineRepository = purchaseLineRepository;
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionDTO> suggestions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);

        // 1. Stock total por produto (soma de todos os armazéns da empresa).
        Map<Long, BigDecimal> stockByProduct = new HashMap<>();
        for (Stock s : stockRepository.findByWarehouseCompanyId(companyId)) {
            Long pid = s.getProduct().getId();
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            stockByProduct.merge(pid, qty, BigDecimal::add);
        }

        // 2. Velocidade de vendas dos últimos 30 dias (faturas emitidas/pagas).
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Map<Long, BigDecimal> salesLast30Days = new HashMap<>();
        List<Object[]> salesAgg = invoiceRepository.sumQuantitySoldByProductSince(
                companyId, InvoiceStatus.realisedSaleStatuses(), thirtyDaysAgo);
        if (salesAgg != null) {
            for (Object[] row : salesAgg) {
                if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
                Long pid = row[0] instanceof Long l ? l : ((Number) row[0]).longValue();
                BigDecimal totalQty = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
                salesLast30Days.put(pid, totalQty);
            }
        }

        // 3. Histórico de compras para identificar fornecedor habitual e preço unitário recente.
        record ProductPurchaseInfo(Long supplierId, String supplierName, BigDecimal unitPrice) {}
        Map<Long, ProductPurchaseInfo> recentPurchaseByProduct = new HashMap<>();
        List<PurchaseLine> recentPurchases = purchaseLineRepository.findRecentPurchasesByCompany(companyId);
        if (recentPurchases != null) {
            for (PurchaseLine pl : recentPurchases) {
                if (pl == null || pl.getProduct() == null) continue;
                Long pid = pl.getProduct().getId();
                // Primeiro que aparecer é o mais recente devido à ordenação descrescente por data
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

        List<ReorderSuggestionDTO> out = new ArrayList<>();
        for (Product p : productRepository.findDistinctByCompaniesIdOrderByName(companyId)) {
            if (!p.isStockTracked()) continue;
            BigDecimal min = p.getMinStock();
            if (min == null || min.signum() <= 0) continue; // sem ponto de reposição definido

            BigDecimal current = stockByProduct.getOrDefault(p.getId(), BigDecimal.ZERO);
            if (current.compareTo(min) >= 0) continue; // stock suficiente

            int upb = p.getUnitsPerBox() <= 0 ? 1 : p.getUnitsPerBox();
            BigDecimal deficit = min.subtract(current);
            // Arredonda para cima a caixas inteiras.
            BigDecimal boxes = deficit.divide(BigDecimal.valueOf(upb), 0, RoundingMode.CEILING);
            BigDecimal suggestedUnits = boxes.multiply(BigDecimal.valueOf(upb));

            // Velocidade média de vendas diária
            BigDecimal totalSold30 = salesLast30Days.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal dailySalesRate = totalSold30.divide(DAYS_WINDOW, 2, RoundingMode.HALF_UP);

            // Previsão de dias de stock restantes
            Integer daysRemaining = null;
            if (current.signum() <= 0) {
                daysRemaining = 0;
            } else if (dailySalesRate.signum() > 0) {
                daysRemaining = current.divide(dailySalesRate, 0, RoundingMode.FLOOR).intValue();
            }

            // Estado de urgência
            String urgencyStatus;
            if (current.signum() <= 0) {
                urgencyStatus = "ESGOTADO";
            } else if (daysRemaining != null && daysRemaining <= 7) {
                urgencyStatus = "CRÍTICO";
            } else {
                urgencyStatus = "BAIXO";
            }

            // Fornecedor habitual e preço unitário estimado
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
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    current,
                    min,
                    upb,
                    boxes,
                    suggestedUnits,
                    dailySalesRate,
                    daysRemaining,
                    urgencyStatus,
                    supplierId,
                    supplierName,
                    estPrice,
                    estTotalCost
            ));
        }

        // Ordenação inteligente:
        // 1. Criticidade: ESGOTADO (1) -> CRÍTICO (2) -> BAIXO (3)
        // 2. Dias restantes (menor primeiro)
        // 3. Menor cobertura (current / min)
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

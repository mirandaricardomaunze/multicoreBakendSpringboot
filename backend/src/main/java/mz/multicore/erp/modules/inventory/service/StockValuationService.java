package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** Value of the stock the company holds, per product, at the product cost (weighted average). */
@Service
public class StockValuationService {

    private final StockRepository stockRepository;

    public StockValuationService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> valueByProduct(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        Map<Long, BigDecimal> values = new HashMap<>();
        for (Stock stock : stockRepository.findByWarehouseCompanyId(companyId)) {
            BigDecimal qty = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
            BigDecimal cost = stock.getProduct().getPurchasePrice() == null ? BigDecimal.ZERO : stock.getProduct().getPurchasePrice();
            if (qty.signum() <= 0 || cost.signum() <= 0 || !stock.getProduct().isStockTracked()) continue;
            values.merge(stock.getProduct().getId(), qty.multiply(cost), BigDecimal::add);
        }
        return values;
    }
}

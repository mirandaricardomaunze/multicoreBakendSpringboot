package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Weighted average cost of a product made in-house (carcass cuts, bakery production): the stock already in the
 * warehouse at the old cost plus the new quantity at its real cost. Call it <b>before</b> the entry movement, so
 * the stock on hand still excludes the new quantity.
 */
@Component
public class AverageCostUpdater {

    private final StockRepository stockRepository;
    private final ProductCommandService productCommands;

    public AverageCostUpdater(StockRepository stockRepository, ProductCommandService productCommands) {
        this.stockRepository = stockRepository;
        this.productCommands = productCommands;
    }

    public void beforeEntry(Product product, Warehouse warehouse, BigDecimal quantity, BigDecimal totalCost) {
        if (quantity == null || quantity.signum() <= 0 || totalCost == null) return;
        WeightedAverageCost.apply(stockRepository, product, warehouse, quantity,
                totalCost.divide(quantity, 6, RoundingMode.HALF_UP));
        productCommands.saveProduct(product);
    }
}

package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
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
    private final ProductRepository productRepository;

    public AverageCostUpdater(StockRepository stockRepository, ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
    }

    public void beforeEntry(Product product, Warehouse warehouse, BigDecimal quantity, BigDecimal totalCost) {
        BigDecimal onHand = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO).max(BigDecimal.ZERO);
        BigDecimal total = onHand.add(quantity);
        if (total.signum() <= 0) return;
        BigDecimal oldCost = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();
        product.setPurchasePrice(onHand.multiply(oldCost).add(totalCost).divide(total, 2, RoundingMode.HALF_UP));
        productRepository.save(product);
    }
}

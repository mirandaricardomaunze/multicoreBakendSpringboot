package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Weighted average cost: the stock the company already holds (every warehouse) at the old cost, plus the new
 * quantity at its real unit cost. The product cost drives the cost of goods sold, wastes and adjustments, so a
 * purchase at a new price must move it. Call it <b>before</b> the entry movement, while the stock on hand still
 * excludes the new quantity. A product never costed (cost zero) or with no stock takes the new cost as it is.
 */
public final class WeightedAverageCost {

    private WeightedAverageCost() {
    }

    public static void apply(StockRepository stockRepository, Product product, Warehouse warehouse,
                             BigDecimal quantity, BigDecimal unitCost) {
        if (unitCost == null || unitCost.signum() < 0 || quantity == null || quantity.signum() <= 0 || product.getId() == null) return;
        Long companyId = warehouse == null || warehouse.getCompany() == null ? null : warehouse.getCompany().getId();
        BigDecimal onHand = stockRepository.findByProductId(product.getId()).stream()
                .filter(stock -> companyId == null || (stock.getWarehouse() != null && stock.getWarehouse().getCompany() != null
                        && companyId.equals(stock.getWarehouse().getCompany().getId())))
                .map(Stock::getQuantity).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO);
        BigDecimal oldCost = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();
        if (onHand.signum() == 0 || oldCost.signum() == 0) {
            product.setPurchasePrice(unitCost.setScale(2, RoundingMode.HALF_UP));
            return;
        }
        product.setPurchasePrice(onHand.multiply(oldCost).add(unitCost.multiply(quantity))
                .divide(onHand.add(quantity), 2, RoundingMode.HALF_UP));
    }
}

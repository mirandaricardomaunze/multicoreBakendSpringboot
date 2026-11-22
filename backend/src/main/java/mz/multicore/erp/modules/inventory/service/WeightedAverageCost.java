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
        BigDecimal onHand = companyOnHand(stockRepository, product, warehouse);
        BigDecimal oldCost = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();
        if (onHand.signum() == 0 || oldCost.signum() == 0) {
            product.setPurchasePrice(unitCost.setScale(2, RoundingMode.HALF_UP));
            return;
        }
        product.setPurchasePrice(onHand.multiply(oldCost).add(unitCost.multiply(quantity))
                .divide(onHand.add(quantity), 2, RoundingMode.HALF_UP));
    }

    /**
     * Custo acrescentado ao stock que já existe, sem entrada de quantidade (custos de importação,
     * MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC §4.4): o valor do stock da empresa sobe {@code addedCost} e o médio passa a
     * {@code (stock × médio + custo) / stock}, 2 casas HALF_UP. Sem stock ou sem custo, nada muda.
     */
    public static void absorb(StockRepository stockRepository, Product product, Warehouse warehouse,
                              BigDecimal addedCost) {
        if (addedCost == null || addedCost.signum() <= 0 || product.getId() == null) return;
        BigDecimal onHand = companyOnHand(stockRepository, product, warehouse);
        if (onHand.signum() == 0) return;
        BigDecimal oldCost = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();
        product.setPurchasePrice(onHand.multiply(oldCost).add(addedCost).divide(onHand, 2, RoundingMode.HALF_UP));
    }

    /** Stock do artigo em todos os armazéns da empresa do armazém indicado (nunca negativo). */
    private static BigDecimal companyOnHand(StockRepository stockRepository, Product product, Warehouse warehouse) {
        Long companyId = warehouse == null || warehouse.getCompany() == null ? null : warehouse.getCompany().getId();
        return stockRepository.findByProductId(product.getId()).stream()
                .filter(stock -> companyId == null || (stock.getWarehouse() != null && stock.getWarehouse().getCompany() != null
                        && companyId.equals(stock.getWarehouse().getCompany().getId())))
                .map(Stock::getQuantity).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO);
    }
}

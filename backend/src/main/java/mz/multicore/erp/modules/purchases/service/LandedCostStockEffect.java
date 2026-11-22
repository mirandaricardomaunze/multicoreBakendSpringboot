package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.AverageCostUpdater;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocation;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Efeito no stock de um documento CI (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.3, §4.4): reparte o custo pelas
 * linhas das compras ligadas e, em cada linha, a parte do stock que ainda existe no armazém sobe o custo médio do
 * artigo; o resto é a parte já vendida, que vai a CMV. Não mexe em quantidades nem em vendas passadas (o custo da
 * linha de venda está fotografado).
 */
@Component
public class LandedCostStockEffect {

    private final StockLookup stockLookup;
    private final AverageCostUpdater averageCost;
    private final LandedCostCalculator calculator = new LandedCostCalculator();

    public LandedCostStockEffect(StockLookup stockLookup, AverageCostUpdater averageCost) {
        this.stockLookup = stockLookup;
        this.averageCost = averageCost;
    }

    /** Reparte {@code totalCost} (moeda base) pelas linhas das compras e aplica cada quota ao stock ou a CMV. */
    public List<LandedCostAllocation> apply(LandedCostAllocationMethod method, BigDecimal totalCost,
                                            List<Purchase> purchases) {
        return allocate(method, totalCost, purchases, (product, warehouse, before, stockAmount) -> {
            averageCost.absorbCost(product, warehouse, stockAmount);
            return unitCost(product);
        });
    }

    /**
     * O mesmo cálculo que {@link #apply}, sem mexer em nenhum artigo (pré-visualização, §4.3): o médio de cada artigo
     * é simulado e acumula quando o mesmo artigo aparece em várias linhas, como faria a confirmação.
     */
    public List<LandedCostAllocation> preview(LandedCostAllocationMethod method, BigDecimal totalCost,
                                              List<Purchase> purchases) {
        Map<Long, BigDecimal> simulated = new HashMap<>();
        return allocate(method, totalCost, purchases, new AverageCost() {
            @Override
            public BigDecimal before(Product product) {
                return simulated.getOrDefault(product.getId(), unitCost(product));
            }

            @Override
            public BigDecimal absorb(Product product, Warehouse warehouse, BigDecimal before, BigDecimal stockAmount) {
                BigDecimal after = averageCost.costAfterAbsorbing(product, warehouse, before, stockAmount)
                        .setScale(2, RoundingMode.HALF_UP);
                simulated.put(product.getId(), after);
                return after;
            }
        });
    }

    /** Como o custo médio de um artigo reage à parte do stock: aplicado de verdade ou só simulado. */
    private interface AverageCost {
        default BigDecimal before(Product product) {
            return unitCost(product);
        }

        BigDecimal absorb(Product product, Warehouse warehouse, BigDecimal before, BigDecimal stockAmount);
    }

    private List<LandedCostAllocation> allocate(LandedCostAllocationMethod method, BigDecimal totalCost,
                                                List<Purchase> purchases, AverageCost cost) {
        List<PurchaseLine> lines = purchases.stream().flatMap(purchase -> purchase.getLines().stream()).toList();
        List<LandedCostCalculator.Allocation> shares = calculator.allocate(totalCost, method,
                lines.stream().map(LandedCostStockEffect::allocationLine).toList());
        List<LandedCostAllocation> result = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            result.add(applyShare(lines.get(i), shares.get(i).share(), cost));
        }
        return List.copyOf(result);
    }

    private LandedCostAllocation applyShare(PurchaseLine line, BigDecimal share, AverageCost cost) {
        Product product = line.getProduct();
        Warehouse warehouse = line.getPurchase().getWarehouse();
        BigDecimal stockQuantity = warehouse == null ? BigDecimal.ZERO
                : stockLookup.quantityOf(product.getId(), warehouse.getId());
        LandedCostCalculator.StockSplit split = calculator.splitByStock(share, line.getQuantity(), stockQuantity);

        LandedCostAllocation allocation = new LandedCostAllocation();
        allocation.setPurchaseLine(line);
        allocation.setReceivedQuantity(line.getQuantity());
        allocation.setStockQuantity(stockQuantity.max(BigDecimal.ZERO));
        allocation.setShareAmount(share);
        allocation.setStockAmount(split.stockAmount());
        allocation.setSoldAmount(split.soldAmount());
        BigDecimal before = cost.before(product);
        allocation.setUnitCostBefore(before);
        allocation.setUnitCostAfter(cost.absorb(product, warehouse, before, split.stockAmount()));
        return allocation;
    }

    /** Base da repartição: valor sem IVA da linha em moeda base, quantidade recebida e peso líquido do artigo. */
    private static LandedCostCalculator.AllocationLine allocationLine(PurchaseLine line) {
        BigDecimal netValue = line.getUnitPrice().multiply(line.getQuantity());
        return new LandedCostCalculator.AllocationLine(line.getId(), line.getPurchase().toBaseAmount(netValue),
                line.getQuantity(), line.getProduct().getNetUnitWeightKg());
    }

    private static BigDecimal unitCost(Product product) {
        BigDecimal cost = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();
        return cost.setScale(2, RoundingMode.HALF_UP);
    }
}

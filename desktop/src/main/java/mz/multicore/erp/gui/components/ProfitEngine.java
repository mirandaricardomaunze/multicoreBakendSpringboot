package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de cálculo de rentabilidade, CMVMC (Custo das Mercadorias Vendidas) e margem bruta.
 */
public class ProfitEngine {

    /** Custo padrão estimado caso o produto não tenha preço de custo definido (65% do preço de venda). */
    public static final BigDecimal DEFAULT_COST_RATIO = new BigDecimal("0.65");

    public record ProfitMetrics(
            BigDecimal totalRevenue,
            BigDecimal totalCogs,
            BigDecimal grossProfit,
            BigDecimal profitMarginPercent,
            BigDecimal averageTicket,
            int transactionCount,
            BigDecimal posRevenue,
            BigDecimal invoiceRevenue
    ) {}

    public static ProfitMetrics calculateMetrics(
            List<InvoiceDTO> invoices,
            BigDecimal posRevenue,
            List<ProductDTO> catalog
    ) {
        Map<String, BigDecimal> productCostMap = new HashMap<>();
        if (catalog != null) {
            for (ProductDTO p : catalog) {
                if (p.name() != null) {
                    BigDecimal cost = (p.purchasePrice() != null && p.purchasePrice().signum() > 0)
                            ? p.purchasePrice()
                            : (p.unitPrice() != null ? p.unitPrice().multiply(DEFAULT_COST_RATIO) : BigDecimal.ZERO);
                    productCostMap.put(p.name().toLowerCase().trim(), cost);
                }
            }
        }

        BigDecimal invoiceRev = BigDecimal.ZERO;
        BigDecimal invoiceCogs = BigDecimal.ZERO;
        int invoiceCount = 0;

        if (invoices != null) {
            for (InvoiceDTO inv : invoices) {
                if (inv.status() == InvoiceStatus.CANCELLED || inv.status() == InvoiceStatus.DRAFT) continue;
                BigDecimal invTotal = inv.totalAmount() != null ? inv.totalAmount() : BigDecimal.ZERO;
                invoiceRev = invoiceRev.add(invTotal);
                invoiceCount++;

                if (inv.lines() != null) {
                    for (InvoiceLineDTO line : inv.lines()) {
                        BigDecimal qty = line.quantity() != null ? line.quantity() : BigDecimal.ONE;
                        BigDecimal cost = getLineUnitCost(line.productName(), line.unitPrice(), productCostMap);
                        invoiceCogs = invoiceCogs.add(cost.multiply(qty));
                    }
                } else {
                    invoiceCogs = invoiceCogs.add(invTotal.multiply(DEFAULT_COST_RATIO));
                }
            }
        }

        BigDecimal posRev = posRevenue != null ? posRevenue : BigDecimal.ZERO;
        BigDecimal posCogs = posRev.multiply(DEFAULT_COST_RATIO);
        int posCount = posRev.signum() > 0 ? 1 : 0;

        BigDecimal totalRevenue = invoiceRev.add(posRev).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCogs = invoiceCogs.add(posCogs).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossProfit = totalRevenue.subtract(totalCogs).setScale(2, RoundingMode.HALF_UP);

        BigDecimal profitMargin = BigDecimal.ZERO;
        if (totalRevenue.signum() > 0) {
            profitMargin = grossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 1, RoundingMode.HALF_UP);
        }

        int totalTx = invoiceCount + posCount;
        BigDecimal avgTicket = BigDecimal.ZERO;
        if (totalTx > 0) {
            avgTicket = totalRevenue.divide(BigDecimal.valueOf(totalTx), 2, RoundingMode.HALF_UP);
        }

        return new ProfitMetrics(
                totalRevenue,
                totalCogs,
                grossProfit,
                profitMargin,
                avgTicket,
                totalTx,
                posRev,
                invoiceRev
        );
    }

    private static BigDecimal getLineUnitCost(String productName, BigDecimal unitPrice, Map<String, BigDecimal> costMap) {
        if (productName != null) {
            BigDecimal knownCost = costMap.get(productName.toLowerCase().trim());
            if (knownCost != null && knownCost.signum() > 0) return knownCost;
        }
        if (unitPrice != null && unitPrice.signum() > 0) {
            return unitPrice.multiply(DEFAULT_COST_RATIO);
        }
        return BigDecimal.ZERO;
    }
}

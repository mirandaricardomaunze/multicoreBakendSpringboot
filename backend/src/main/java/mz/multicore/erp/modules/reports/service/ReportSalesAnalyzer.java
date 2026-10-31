package mz.multicore.erp.modules.reports.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.reports.dto.OperatorSalesSummaryDTO;
import mz.multicore.erp.modules.reports.dto.ProductMarginDTO;
import mz.multicore.erp.modules.reports.dto.SalesSummaryDTO;
import mz.multicore.erp.modules.reports.dto.TopProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Cálculos puros de vendas, artigos, operadores e margem histórica. */
@Component
public class ReportSalesAnalyzer {
    private static final int TOP_PRODUCTS_LIMIT = 5;

    List<Invoice> filterRealisedSales(List<Invoice> invoices) {
        return invoices.stream().filter(invoice -> invoice.getStatus().isRealisedSale()).toList();
    }

    SalesSummaryDTO summarise(List<Invoice> sales) {
        BigDecimal total = sales.stream().map(Invoice::getTotalAmount).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesSummaryDTO(sales.size(), total);
    }

    List<TopProductDTO> topProductsFrom(List<Invoice> sales) {
        Map<Long, BigDecimal> quantities = new HashMap<>();
        Map<Long, BigDecimal> revenues = new HashMap<>();
        Map<Long, String[]> names = new HashMap<>();
        for (Invoice invoice : sales) {
            for (InvoiceLine line : invoice.getLines()) {
                Long productId = line.getProduct().getId();
                quantities.merge(productId, valueOrZero(line.getQuantity()), BigDecimal::add);
                revenues.merge(productId, valueOrZero(line.getLineTotal()), BigDecimal::add);
                names.putIfAbsent(productId,
                        new String[]{line.getProduct().getSku(), line.getProduct().getName()});
            }
        }
        return quantities.entrySet().stream()
                .map(entry -> new TopProductDTO(entry.getKey(), names.get(entry.getKey())[0],
                        names.get(entry.getKey())[1], entry.getValue(),
                        revenues.getOrDefault(entry.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparing(TopProductDTO::quantitySold).reversed())
                .limit(TOP_PRODUCTS_LIMIT).toList();
    }

    BigDecimal unpaidInvoicesTotal(List<Invoice> invoices) {
        return invoices.stream().filter(invoice -> invoice.getStatus().isCollectable())
                .map(Invoice::outstandingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    BigDecimal outstandingCreditFor(List<Invoice> invoices) {
        return invoices.stream().map(Invoice::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    List<OperatorSalesSummaryDTO> salesByOperator(List<Invoice> sales) {
        Map<String, List<Invoice>> byOperator = sales.stream().collect(Collectors.groupingBy(invoice ->
                invoice.getCreatedBy() == null || invoice.getCreatedBy().isBlank()
                        ? "SYSTEM" : invoice.getCreatedBy()));
        return byOperator.entrySet().stream()
                .map(entry -> new OperatorSalesSummaryDTO(entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream().map(invoice -> valueOrZero(invoice.getTotalAmount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(OperatorSalesSummaryDTO::total).reversed()).toList();
    }

    List<ProductMarginDTO> marginByProduct(List<Invoice> sales) {
        Map<Long, ProductMarginAccumulator> accumulators = new HashMap<>();
        for (Invoice invoice : sales) {
            for (InvoiceLine line : invoice.getLines()) {
                Long productId = line.getProduct().getId();
                ProductMarginAccumulator current = accumulators.computeIfAbsent(productId,
                        id -> new ProductMarginAccumulator(line.getProduct().getSku(), line.getProduct().getName()));
                current.revenue = current.revenue.add(valueOrZero(line.getLineTotal()));
                current.estimatedCost = current.estimatedCost.add(line.lineCost());
            }
        }
        return accumulators.entrySet().stream()
                .map(entry -> new ProductMarginDTO(entry.getKey(), entry.getValue().sku,
                        entry.getValue().name, entry.getValue().revenue, entry.getValue().estimatedCost,
                        entry.getValue().revenue.subtract(entry.getValue().estimatedCost)))
                .sorted(Comparator.comparing(ProductMarginDTO::grossMargin).reversed()).toList();
    }

    private BigDecimal valueOrZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private static final class ProductMarginAccumulator {
        private final String sku;
        private final String name;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal estimatedCost = BigDecimal.ZERO;

        private ProductMarginAccumulator(String sku, String name) {
            this.sku = sku;
            this.name = name;
        }
    }
}

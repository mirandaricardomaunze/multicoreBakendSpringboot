package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceLineDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Funções puras de cálculo e agregação para o painel de TV Executiva.
 */
public final class ExecutiveTvCalculationHelper {

    private ExecutiveTvCalculationHelper() {}

    public static List<InvoiceDTO> filterInvoices(List<InvoiceDTO> allInvoices, ExecutiveTvDashboardDialog.Period period) {
        if (allInvoices == null) return List.of();
        LocalDate now = LocalDate.now();
        return allInvoices.stream()
                .filter(inv -> {
                    if (inv.createdAt() == null) return false;
                    LocalDate d = inv.createdAt().toLocalDate();
                    return switch (period) {
                        case HOJE -> d.isEqual(now);
                        case ESTA_SEMANA -> d.isAfter(now.minusDays(7)) || d.isEqual(now.minusDays(7));
                        case ESTE_MES -> d.getYear() == now.getYear() && d.getMonth() == now.getMonth();
                        case ESTE_ANO -> d.getYear() == now.getYear();
                    };
                })
                .toList();
    }

    public static BigDecimal calculateTotalBilled(List<InvoiceDTO> filteredInvoices, Long businessUnitId) {
        if (filteredInvoices == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceDTO inv : filteredInvoices) {
            if (businessUnitId == null) {
                if (inv.businessUnitId() == null || BusinessUnitGoalManager.isUnitIncluded(inv.businessUnitId())) {
                    if (inv.totalAmount() != null) {
                        total = total.add(inv.totalAmount());
                    }
                }
            } else if (businessUnitId.equals(inv.businessUnitId())) {
                if (inv.totalAmount() != null) {
                    total = total.add(inv.totalAmount());
                }
            }
        }
        return total;
    }

    public record ProductMetric(String name, BigDecimal revenue, BigDecimal quantity) {}

    public static List<ProductMetric> aggregateTopProducts(List<InvoiceDTO> invoices, Long businessUnitId) {
        if (invoices == null) return List.of();
        Map<String, BigDecimal> productRevenue = new HashMap<>();
        Map<String, BigDecimal> productQty = new HashMap<>();

        for (InvoiceDTO inv : invoices) {
            if (businessUnitId != null && !businessUnitId.equals(inv.businessUnitId())) continue;
            if (inv.lines() != null) {
                for (InvoiceLineDTO line : inv.lines()) {
                    String name = line.productName() != null ? line.productName() : "Artigo";
                    BigDecimal rev = line.lineTotal() != null ? line.lineTotal() : BigDecimal.ZERO;
                    BigDecimal q = line.quantity() != null ? line.quantity() : BigDecimal.ZERO;
                    productRevenue.put(name, productRevenue.getOrDefault(name, BigDecimal.ZERO).add(rev));
                    productQty.put(name, productQty.getOrDefault(name, BigDecimal.ZERO).add(q));
                }
            }
        }

        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(productRevenue.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<ProductMetric> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sorted) {
            String name = entry.getKey();
            result.add(new ProductMetric(name, entry.getValue(), productQty.getOrDefault(name, BigDecimal.ZERO)));
        }
        return result;
    }
}

package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.inventory.dto.ExpiringBatchAlertDTO;
import mz.multicore.erp.modules.inventory.dto.WasteSummaryDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Radar de lotes com validade a expirar e resumo analítico de quebras com rácio sobre vendas.
 */
@Service
public class StockWasteAnalyticsService {

    private final ProductBatchRepository batchRepository;
    private final StockWasteRepository wasteRepository;
    private final InvoiceLookup invoiceLookup;

    public StockWasteAnalyticsService(
            ProductBatchRepository batchRepository,
            StockWasteRepository wasteRepository,
            InvoiceLookup invoiceLookup
    ) {
        this.batchRepository = batchRepository;
        this.wasteRepository = wasteRepository;
        this.invoiceLookup = invoiceLookup;
    }

    @Transactional(readOnly = true)
    public List<ExpiringBatchAlertDTO> getExpiringBatchesRadar(Long companyId, int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        List<ProductBatch> batches = batchRepository.findExpiringByCompanyId(companyId, cutoff);

        return batches.stream().map(b -> {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), b.getExpirationDate());
            String alertLevel;
            if (daysUntil < 0) {
                alertLevel = "VENCIDO";
            } else if (daysUntil <= 3) {
                alertLevel = "CRÍTICO";
            } else if (daysUntil <= 7) {
                alertLevel = "ALTO";
            } else if (daysUntil <= 15) {
                alertLevel = "MÉDIO";
            } else {
                alertLevel = "ATENÇÃO";
            }

            BigDecimal unitCost = StockWasteLifecycleService.resolveUnitCost(b.getProduct());
            BigDecimal potentialLoss = unitCost.multiply(b.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            String category = b.getProduct().getCategory() != null ? b.getProduct().getCategory().getName() : "Geral";

            return new ExpiringBatchAlertDTO(
                    b.getId(),
                    b.getBatchNumber(),
                    b.getProduct().getId(),
                    b.getProduct().getSku(),
                    b.getProduct().getName(),
                    category,
                    b.getWarehouse().getId(),
                    b.getWarehouse().getName(),
                    b.getQuantity(),
                    unitCost,
                    potentialLoss,
                    b.getExpirationDate(),
                    daysUntil,
                    alertLevel
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public WasteSummaryDTO getSummary(Long companyId, LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        List<StockWaste> records = wasteRepository.findByCompanyIdAndPeriod(companyId, startDt, endDt);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        long totalCount = 0;

        Map<WasteReason, BigDecimal> costByReason = new EnumMap<>(WasteReason.class);
        for (WasteReason r : WasteReason.values()) {
            costByReason.put(r, BigDecimal.ZERO);
        }

        Map<String, BigDecimal> costByCategory = new HashMap<>();

        for (StockWaste w : records) {
            if (w.getStatus() == WasteStatus.APPROVED) {
                totalCost = totalCost.add(w.getTotalCost());
                totalQty = totalQty.add(w.getQuantity());
                totalCount++;

                costByReason.merge(w.getReason(), w.getTotalCost(), BigDecimal::add);

                String catName = (w.getProduct() != null && w.getProduct().getCategory() != null)
                        ? w.getProduct().getCategory().getName()
                        : "Outro";
                costByCategory.merge(catName, w.getTotalCost(), BigDecimal::add);
            }
        }

        List<Invoice> invoices = invoiceLookup.listCreatedBetween(companyId, startDt, endDt);
        BigDecimal totalRevenue = invoices.stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal wasteRate = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            wasteRate = totalCost.multiply(new BigDecimal("100"))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        return new WasteSummaryDTO(
                totalCost,
                totalQty,
                totalCount,
                totalRevenue,
                wasteRate,
                costByReason,
                costByCategory
        );
    }
}

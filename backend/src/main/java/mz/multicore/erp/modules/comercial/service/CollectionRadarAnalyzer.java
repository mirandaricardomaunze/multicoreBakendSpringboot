package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Analisa facturas em aberto e classifica-as nas faixas do Radar Preventivo de Cobranças. */
@Component
public class CollectionRadarAnalyzer {

    private final InvoiceRepository invoiceRepository;

    public CollectionRadarAnalyzer(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public CollectionRadarSummaryDTO buildSummary(Long companyId, LocalDate ref) {
        List<Invoice> collectable = loadCollectable(companyId);

        BigDecimal critTotal = BigDecimal.ZERO;
        int critCount = 0;
        BigDecimal recTotal = BigDecimal.ZERO;
        int recCount = 0;
        BigDecimal todayTotal = BigDecimal.ZERO;
        int todayCount = 0;
        BigDecimal in7Total = BigDecimal.ZERO;
        int in7Count = 0;
        BigDecimal in15Total = BigDecimal.ZERO;
        int in15Count = 0;
        BigDecimal in30Total = BigDecimal.ZERO;
        int in30Count = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Invoice invoice : collectable) {
            BigDecimal balance = invoice.outstandingAmount();
            grandTotal = grandTotal.add(balance);
            LocalDate due = invoice.effectiveDueDate();
            int daysUntilDue = due != null ? (int) ChronoUnit.DAYS.between(ref, due) : 0;
            CollectionRadarBucket bucket = CollectionRadarBucket.of(daysUntilDue);

            switch (bucket) {
                case CRITICAL_OVERDUE -> { critTotal = critTotal.add(balance); critCount++; }
                case RECENT_OVERDUE -> { recTotal = recTotal.add(balance); recCount++; }
                case DUE_TODAY -> { todayTotal = todayTotal.add(balance); todayCount++; }
                case DUE_IN_7_DAYS -> { in7Total = in7Total.add(balance); in7Count++; }
                case DUE_IN_15_DAYS -> { in15Total = in15Total.add(balance); in15Count++; }
                case DUE_IN_30_DAYS -> { in30Total = in30Total.add(balance); in30Count++; }
                case FUTURE -> { }
            }
        }

        return new CollectionRadarSummaryDTO(
                companyId, ref,
                critTotal, critCount,
                recTotal, recCount,
                todayTotal, todayCount,
                in7Total, in7Count,
                in15Total, in15Count,
                in30Total, in30Count,
                grandTotal, collectable.size()
        );
    }

    public List<CollectionRadarItemDTO> buildItems(Long companyId, CollectionRadarBucket filterBucket, LocalDate ref) {
        List<Invoice> collectable = loadCollectable(companyId);
        List<CollectionRadarItemDTO> items = new ArrayList<>();

        for (Invoice invoice : collectable) {
            LocalDate due = invoice.effectiveDueDate();
            int daysUntilDue = due != null ? (int) ChronoUnit.DAYS.between(ref, due) : 0;
            CollectionRadarBucket bucket = CollectionRadarBucket.of(daysUntilDue);
            if (filterBucket != null && bucket != filterBucket) {
                continue;
            }
            Client client = invoice.getClient();
            items.add(new CollectionRadarItemDTO(
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    client != null ? client.getId() : null,
                    client != null ? client.getName() : "Cliente sem ficha",
                    client != null ? client.getTaxId() : "-",
                    "-",
                    client != null ? client.getEmail() : "-",
                    invoice.issueDate(),
                    due,
                    invoice.payableTotal(),
                    invoice.getAmountPaid(),
                    invoice.outstandingAmount(),
                    daysUntilDue,
                    bucket
            ));
        }

        items.sort(Comparator.comparingInt(CollectionRadarItemDTO::daysUntilDue));
        return items;
    }

    private List<Invoice> loadCollectable(Long companyId) {
        return invoiceRepository.findOutstandingWithClient(companyId, InvoiceStatus.collectableStatuses()).stream()
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .toList();
    }
}

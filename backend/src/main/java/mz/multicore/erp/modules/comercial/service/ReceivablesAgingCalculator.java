package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.AgingBucketTotalDTO;
import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.ClientAgingDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReceivablesAgingCalculator {

    private final InvoiceRepository invoiceRepository;

    public ReceivablesAgingCalculator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public AgingSummaryDTO getAging(LocalDate referenceDate) {
        LocalDate reference = referenceDate == null ? LocalDate.now() : referenceDate;
        Long companyId = CurrentUserContext.requireCurrentCompanyId();

        List<Invoice> collectable = invoiceRepository.findOutstandingWithClient(
                companyId, InvoiceStatus.collectableStatuses()).stream()
                .filter(invoice -> invoice.getStatus().isCollectable())
                .filter(invoice -> invoice.outstandingAmount().signum() > 0)
                .toList();

        Map<AgingBucket, BigDecimal> bucketTotals = new EnumMap<>(AgingBucket.class);
        Map<AgingBucket, Integer> bucketCounts = new EnumMap<>(AgingBucket.class);
        Map<Long, ClientAging> byClient = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal overdueTotal = BigDecimal.ZERO;

        for (Invoice invoice : collectable) {
            BigDecimal outstanding = invoice.outstandingAmount();
            AgingBucket bucket = invoice.agingBucket(reference);

            bucketTotals.merge(bucket, outstanding, BigDecimal::add);
            bucketCounts.merge(bucket, 1, Integer::sum);
            total = total.add(outstanding);
            if (bucket.isOverdue()) {
                overdueTotal = overdueTotal.add(outstanding);
            }

            Client client = invoice.getClient();
            Long clientId = client == null ? null : client.getId();
            byClient.computeIfAbsent(clientId, id -> new ClientAging(client))
                    .add(bucket, outstanding, invoice.effectiveDueDate(), invoice.daysOverdue(reference));
        }

        List<AgingBucketTotalDTO> buckets = new ArrayList<>();
        for (AgingBucket bucket : AgingBucket.values()) {
            buckets.add(new AgingBucketTotalDTO(bucket, bucket.label(),
                    bucketTotals.getOrDefault(bucket, BigDecimal.ZERO),
                    bucketCounts.getOrDefault(bucket, 0)));
        }

        List<ClientAgingDTO> clients = byClient.values().stream()
                .map(ClientAging::toDTO)
                .sorted(Comparator.comparing(ClientAgingDTO::total).reversed())
                .toList();

        return new AgingSummaryDTO(reference, buckets, clients, total, overdueTotal);
    }

    private static final class ClientAging {

        private final Long id;
        private final String name;
        private final String taxId;
        private final Map<AgingBucket, BigDecimal> totals = new EnumMap<>(AgingBucket.class);
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal overdue = BigDecimal.ZERO;
        private LocalDate oldestDueDate;
        private int maxDaysOverdue;

        private ClientAging(Client client) {
            this.id = client == null ? null : client.getId();
            this.name = client == null ? "(sem cliente)" : client.getName();
            this.taxId = client == null ? null : client.getTaxId();
        }

        private void add(AgingBucket bucket, BigDecimal amount, LocalDate dueDate, int daysOverdue) {
            totals.merge(bucket, amount, BigDecimal::add);
            total = total.add(amount);
            if (bucket.isOverdue()) {
                overdue = overdue.add(amount);
            }
            if (dueDate != null && (oldestDueDate == null || dueDate.isBefore(oldestDueDate))) {
                oldestDueDate = dueDate;
            }
            maxDaysOverdue = Math.max(maxDaysOverdue, daysOverdue);
        }

        private BigDecimal at(AgingBucket bucket) {
            return totals.getOrDefault(bucket, BigDecimal.ZERO);
        }

        private ClientAgingDTO toDTO() {
            return new ClientAgingDTO(id, name, taxId,
                    at(AgingBucket.CORRENTE), at(AgingBucket.ATE_30), at(AgingBucket.DE_31_A_60),
                    at(AgingBucket.DE_61_A_90), at(AgingBucket.MAIS_DE_90),
                    total, overdue, oldestDueDate, maxDaysOverdue);
        }
    }
}

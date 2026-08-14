package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.dto.AgingBucketTotalDTO;
import com.phcpro.modules.comercial.dto.AgingSummaryDTO;
import com.phcpro.modules.comercial.dto.ClientAgingDTO;
import com.phcpro.modules.comercial.model.AgingBucket;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.repository.InvoiceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contas a receber: quem deve, quanto e <b>há quanto tempo</b>.
 *
 * <p>Serviço próprio, e não mais um método no {@code ComercialService} (já com ~1.100 linhas):
 * a cobrança é uma responsabilidade autónoma — lê faturas, não as emite (SRP).
 *
 * <p>Não reimplementa nenhuma regra: o saldo vem de {@code Invoice.outstandingAmount()}, o atraso
 * de {@code Invoice.daysOverdue(...)} e a classificação de {@code AgingBucket.of(...)}.
 */
@Service
public class ReceivablesService {

    private final InvoiceRepository invoiceRepository;

    public ReceivablesService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /** Mapa de antiguidade da empresa activa à data de hoje. */
    @Transactional(readOnly = true)
    public AgingSummaryDTO getAging() {
        return getAging(LocalDate.now());
    }

    /**
     * Mapa de antiguidade a uma data de referência. A data é parâmetro (e não {@code now()} lá
     * dentro) para o relatório ser reproduzível e testável sem mexer no relógio.
     */
    @Transactional(readOnly = true)
    public AgingSummaryDTO getAging(LocalDate referenceDate) {
        LocalDate reference = referenceDate == null ? LocalDate.now() : referenceDate;
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        List<Invoice> collectable = invoiceRepository.findByCompanyId(companyId).stream()
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

    /** Acumulador por cliente — mantém a soma por escalão sem expor a entidade para fora. */
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

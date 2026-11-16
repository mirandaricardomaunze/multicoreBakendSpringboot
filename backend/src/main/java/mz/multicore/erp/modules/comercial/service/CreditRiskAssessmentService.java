package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.service.ApprovalRequestLookup;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditRiskLevel;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Avaliação e apuração de risco de crédito, faixas de maturidade (aging) e resumo executivo.
 */
@Service
public class CreditRiskAssessmentService {

    public static final String APPROVAL_DOC_TYPE_CREDIT_EXCEPTION = "CREDIT_EXCEPTION";
    public static final int MAX_ALLOWED_OVERDUE_DAYS = 30;

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ApprovalRequestLookup approvalRequestLookup;

    public CreditRiskAssessmentService(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            ApprovalRequestLookup approvalRequestLookup
    ) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.approvalRequestLookup = approvalRequestLookup;
    }

    @Transactional(readOnly = true)
    public CreditRiskSummaryDTO getSummary(LocalDate referenceDate) {
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        List<ClientCreditRiskDTO> clients = getClientsRisk(refDate);

        BigDecimal totalReceivable = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;
        BigDecimal totalCreditLimit = BigDecimal.ZERO;
        int blockedCount = 0;
        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        for (ClientCreditRiskDTO c : clients) {
            totalReceivable = totalReceivable.add(c.totalDebt());
            totalOverdue = totalOverdue.add(c.totalOverdue());
            if (c.creditLimit() != null) {
                totalCreditLimit = totalCreditLimit.add(c.creditLimit());
            }
            if (c.isBlocked()) {
                blockedCount++;
            }
            switch (c.riskLevel()) {
                case CRITICAL -> criticalCount++;
                case HIGH -> highCount++;
                case MEDIUM -> mediumCount++;
                case LOW -> lowCount++;
            }
        }

        return new CreditRiskSummaryDTO(
                refDate,
                totalReceivable,
                totalOverdue,
                totalCreditLimit,
                clients.size(),
                blockedCount,
                criticalCount,
                highCount,
                mediumCount,
                lowCount
        );
    }

    @Transactional(readOnly = true)
    public List<ClientCreditRiskDTO> getClientsRisk(LocalDate referenceDate) {
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        List<Invoice> collectable = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .toList();

        Map<Long, ClientRiskAccumulator> byClient = new LinkedHashMap<>();

        List<Client> allClients = clientRepository.findDistinctByCompaniesIdOrderByName(companyId);
        for (Client c : allClients) {
            byClient.put(c.getId(), new ClientRiskAccumulator(c));
        }

        for (Invoice inv : collectable) {
            Client c = inv.getClient();
            if (c == null) continue;
            ClientRiskAccumulator acc = byClient.computeIfAbsent(c.getId(), id -> new ClientRiskAccumulator(c));
            acc.add(inv, refDate);
        }

        List<ApprovalRequest> activeExceptions = approvalRequestLookup
                .listForCompanyByStatus(companyId, ApprovalStatus.APPROVED).stream()
                .filter(req -> APPROVAL_DOC_TYPE_CREDIT_EXCEPTION.equalsIgnoreCase(req.getDocumentType()))
                .toList();
        Map<Long, Boolean> approvedExceptionClients = new LinkedHashMap<>();
        for (ApprovalRequest req : activeExceptions) {
            approvedExceptionClients.put(req.getDocumentId(), true);
        }

        List<ClientCreditRiskDTO> result = new ArrayList<>();
        for (ClientRiskAccumulator acc : byClient.values()) {
            if (acc.totalDebt.signum() == 0 && acc.client.getCreditLimit() == null) {
                continue;
            }
            boolean hasApprovedException = approvedExceptionClients.getOrDefault(acc.client.getId(), false);
            result.add(acc.toDTO(hasApprovedException));
        }

        result.sort(Comparator.comparing(ClientCreditRiskDTO::totalOverdue)
                .thenComparing(ClientCreditRiskDTO::totalDebt).reversed());

        return result;
    }

    public static String formatMoney(BigDecimal val) {
        BigDecimal safe = val == null ? BigDecimal.ZERO : val;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static final class ClientRiskAccumulator {
        private final Client client;
        private final Map<AgingBucket, BigDecimal> bucketTotals = new EnumMap<>(AgingBucket.class);
        private BigDecimal totalDebt = BigDecimal.ZERO;
        private BigDecimal totalOverdue = BigDecimal.ZERO;
        private LocalDate oldestDueDate;
        private int maxDaysOverdue;

        private ClientRiskAccumulator(Client client) {
            this.client = client;
            for (AgingBucket b : AgingBucket.values()) {
                bucketTotals.put(b, BigDecimal.ZERO);
            }
        }

        private void add(Invoice inv, LocalDate refDate) {
            BigDecimal outstanding = inv.outstandingAmount();
            AgingBucket bucket = inv.agingBucket(refDate);
            bucketTotals.merge(bucket, outstanding, BigDecimal::add);
            totalDebt = totalDebt.add(outstanding);

            if (bucket.isOverdue()) {
                totalOverdue = totalOverdue.add(outstanding);
            }

            LocalDate due = inv.effectiveDueDate();
            if (due != null && (oldestDueDate == null || due.isBefore(oldestDueDate))) {
                oldestDueDate = due;
            }

            int days = inv.daysOverdue(refDate);
            if (days > maxDaysOverdue) {
                maxDaysOverdue = days;
            }
        }

        private ClientCreditRiskDTO toDTO(boolean hasApprovedException) {
            BigDecimal limit = client.getCreditLimit();
            BigDecimal available = client.creditAvailable(totalDebt);

            BigDecimal corrente = bucketTotals.getOrDefault(AgingBucket.CORRENTE, BigDecimal.ZERO);
            BigDecimal ate30 = bucketTotals.getOrDefault(AgingBucket.ATE_30, BigDecimal.ZERO);
            BigDecimal de31a60 = bucketTotals.getOrDefault(AgingBucket.DE_31_A_60, BigDecimal.ZERO);
            BigDecimal de61a90 = bucketTotals.getOrDefault(AgingBucket.DE_61_A_90, BigDecimal.ZERO);
            BigDecimal maisDe90 = bucketTotals.getOrDefault(AgingBucket.MAIS_DE_90, BigDecimal.ZERO);

            CreditRiskLevel level;
            boolean limitExceeded = limit != null && totalDebt.compareTo(limit) > 0;
            BigDecimal limit75Percent = limit != null ? limit.multiply(new BigDecimal("0.75")) : null;

            if (maxDaysOverdue > 60 || maisDe90.signum() > 0) {
                level = CreditRiskLevel.CRITICAL;
            } else if (maxDaysOverdue > 30 || limitExceeded) {
                level = CreditRiskLevel.HIGH;
            } else if (maxDaysOverdue > 0 || (limit75Percent != null && totalDebt.compareTo(limit75Percent) >= 0)) {
                level = CreditRiskLevel.MEDIUM;
            } else {
                level = CreditRiskLevel.LOW;
            }

            boolean blocked = false;
            String reason = null;

            if (!hasApprovedException) {
                if (maxDaysOverdue > MAX_ALLOWED_OVERDUE_DAYS) {
                    blocked = true;
                    reason = String.format("Facturas em mora há mais de %d dias (atraso máximo: %d dias)",
                            MAX_ALLOWED_OVERDUE_DAYS, maxDaysOverdue);
                } else if (limitExceeded) {
                    blocked = true;
                    reason = String.format("Limite de crédito excedido em %s MT",
                            formatMoney(totalDebt.subtract(limit)));
                }
            } else if (maxDaysOverdue > MAX_ALLOWED_OVERDUE_DAYS || limitExceeded) {
                reason = "Excepção autorizada pela gerência";
            }

            return new ClientCreditRiskDTO(
                    client.getId(),
                    client.getName(),
                    client.getTaxId(),
                    client.getEmail(),
                    client.getAddress(),
                    limit,
                    totalDebt,
                    available,
                    oldestDueDate,
                    maxDaysOverdue,
                    corrente,
                    ate30,
                    de31a60,
                    de61a90,
                    maisDe90,
                    totalOverdue,
                    level,
                    blocked,
                    reason
            );
        }
    }
}

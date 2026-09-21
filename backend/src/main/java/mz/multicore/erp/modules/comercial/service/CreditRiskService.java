package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import mz.multicore.erp.modules.comercial.dto.OverdueInvoiceItemDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditRiskLevel;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço executivo de gestão de risco de crédito, matriz de aging e cobrança formal.
 */
@Service
public class CreditRiskService {

    public static final String APPROVAL_DOC_TYPE_CREDIT_EXCEPTION = "CREDIT_EXCEPTION";
    public static final int MAX_ALLOWED_OVERDUE_DAYS = 30;

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalService approvalService;
    private final CompanyService companyService;

    public CreditRiskService(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalService approvalService,
            CompanyService companyService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalService = approvalService;
        this.companyService = companyService;
    }

    /**
     * Resumo executivo da carteira de crédito para o dashboard e tomadores de decisão.
     */
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

    /**
     * Lista detalhada de risco e aging de todos os clientes com dívida ou limite activo.
     */
    @Transactional(readOnly = true)
    public List<ClientCreditRiskDTO> getClientsRisk(LocalDate referenceDate) {
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        List<Invoice> collectable = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .toList();

        Map<Long, ClientRiskAccumulator> byClient = new LinkedHashMap<>();

        // 1. Carregar todos os clientes da empresa
        List<Client> allClients = clientRepository.findDistinctByCompaniesIdOrderByName(companyId);
        for (Client c : allClients) {
            byClient.put(c.getId(), new ClientRiskAccumulator(c));
        }

        // 2. Acumular facturas
        for (Invoice inv : collectable) {
            Client c = inv.getClient();
            if (c == null) continue;
            ClientRiskAccumulator acc = byClient.computeIfAbsent(c.getId(), id -> new ClientRiskAccumulator(c));
            acc.add(inv, refDate);
        }

        // 3. Buscar aprovações activas de excepção de crédito
        List<ApprovalRequest> activeExceptions = approvalRequestRepository
                .findByCompanyIdAndStatus(companyId, ApprovalStatus.APPROVED).stream()
                .filter(req -> APPROVAL_DOC_TYPE_CREDIT_EXCEPTION.equalsIgnoreCase(req.getDocumentType()))
                .toList();
        Map<Long, Boolean> approvedExceptionClients = new LinkedHashMap<>();
        for (ApprovalRequest req : activeExceptions) {
            approvedExceptionClients.put(req.getDocumentId(), true);
        }

        // 4. Transformar em DTOs e filtrar apenas clientes com dívida ou limite definido
        List<ClientCreditRiskDTO> result = new ArrayList<>();
        for (ClientRiskAccumulator acc : byClient.values()) {
            if (acc.totalDebt.signum() == 0 && acc.client.getCreditLimit() == null) {
                continue; // Cliente sem saldo e sem limite atribuído
            }
            boolean hasApprovedException = approvedExceptionClients.getOrDefault(acc.client.getId(), false);
            result.add(acc.toDTO(hasApprovedException));
        }

        result.sort(Comparator.comparing(ClientCreditRiskDTO::totalOverdue)
                .thenComparing(ClientCreditRiskDTO::totalDebt).reversed());

        return result;
    }

    /**
     * Valida se a concessão de crédito é permitida tendo em conta mora e limite financeiro.
     * Dispara BusinessRuleException caso o cliente esteja em mora excessiva ou limite esgotado,
     * a menos que haja aprovação de excepção de crédito.
     */
    @Transactional(readOnly = true)
    public void assertCreditAllowed(Client client, BigDecimal newDebt, LocalDate referenceDate) {
        if (client == null) return;
        BigDecimal debtToAdd = newDebt == null ? BigDecimal.ZERO : newDebt;
        if (debtToAdd.signum() <= 0) return; // Pronto pagamento não é bloqueado

        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        // 1. Verificar se existe excepção aprovada
        List<ApprovalRequest> approvedExceptions = approvalRequestRepository
                .findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                        companyId, APPROVAL_DOC_TYPE_CREDIT_EXCEPTION, client.getId(), ApprovalStatus.APPROVED);

        if (!approvedExceptions.isEmpty()) {
            return; // Excepção concedida pela administração
        }

        // 2. Calcular dívida e atrasos actuais
        List<Invoice> clientInvoices = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(inv -> inv.getClient() != null && client.getId().equals(inv.getClient().getId()))
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .toList();

        BigDecimal currentDebt = BigDecimal.ZERO;
        int maxDaysOverdue = 0;

        for (Invoice inv : clientInvoices) {
            currentDebt = currentDebt.add(inv.outstandingAmount());
            int days = inv.daysOverdue(refDate);
            if (days > maxDaysOverdue) {
                maxDaysOverdue = days;
            }
        }

        // 3. Regra de Mora: cliente com facturas vencidas há mais de 30 dias não pode comprar a crédito
        if (maxDaysOverdue > MAX_ALLOWED_OVERDUE_DAYS) {
            throw new BusinessRuleException(String.format(
                    "Venda a crédito bloqueada para %s. O cliente possui dívida em mora com atraso de %d dias "
                            + "(limite permitido: %d dias). Requer regularização prévia ou aprovação de excepção pela gerência.",
                    client.getName(), maxDaysOverdue, MAX_ALLOWED_OVERDUE_DAYS));
        }

        // 4. Regra de Limite Financeiro
        if (client.hasCreditLimit() && client.exceedsCreditLimit(currentDebt, debtToAdd)) {
            BigDecimal available = client.creditAvailable(currentDebt);
            throw new BusinessRuleException(String.format(
                    "Limite de crédito excedido para %s. Limite: %s MT · Em dívida: %s MT · "
                            + "Disponível: %s MT · Esta venda a crédito: %s MT. "
                            + "Submeta um pedido de excepção à gerência para autorização.",
                    client.getName(), formatMoney(client.getCreditLimit()), formatMoney(currentDebt),
                    formatMoney(available), formatMoney(debtToAdd)));
        }
    }

    /**
     * Submete um pedido formal de excepção de crédito para aprovação pela Direcção/Gerência.
     */
    @Transactional
    public ApprovalRequestDTO requestCreditException(CreditExceptionApprovalRequest req) {
        if (req.clientId() == null) {
            throw new BusinessRuleException("Cliente obrigatório para o pedido de excepção de crédito.");
        }
        if (req.requestedAmount() == null || req.requestedAmount().signum() <= 0) {
            throw new BusinessRuleException("Valor do crédito requerido deve ser positivo.");
        }
        if (req.reason() == null || req.reason().trim().length() < 10) {
            throw new BusinessRuleException("Justificação obrigatória com pelo menos 10 caracteres.");
        }

        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Client client = clientRepository.findByIdAndCompaniesId(req.clientId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado na empresa actual."));

        String desc = String.format("Excepção de Crédito para %s (NUIT %s). Solicitado por %s: %s MT. Motivo: %s",
                client.getName(),
                client.getTaxId(),
                req.salesperson() != null ? req.salesperson() : CurrentUserContext.getUsername(),
                formatMoney(req.requestedAmount()),
                req.reason().trim()
        );

        return approvalService.submitRequest(
                APPROVAL_DOC_TYPE_CREDIT_EXCEPTION,
                client.getId(),
                req.requestedAmount(),
                desc
        );
    }

    /**
     * Prepara os dados consolidados para a Notificação Formal de Cobrança de um cliente.
     */
    @Transactional(readOnly = true)
    public DebtCollectionNoticeDTO getDebtCollectionNotice(Long clientId, LocalDate referenceDate) {
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        Client client = clientRepository.findByIdAndCompaniesId(clientId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado na empresa actual: " + clientId));

        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        List<Invoice> overdueInvoices = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(inv -> inv.getClient() != null && clientId.equals(inv.getClient().getId()))
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .filter(inv -> inv.daysOverdue(refDate) > 0)
                .sorted(Comparator.comparing(Invoice::effectiveDueDate))
                .toList();

        if (overdueInvoices.isEmpty()) {
            throw new BusinessRuleException(String.format("O cliente %s não possui facturas vencidas em mora.", client.getName()));
        }

        BigDecimal totalOverdue = BigDecimal.ZERO;
        int maxOverdue = 0;
        List<OverdueInvoiceItemDTO> items = new ArrayList<>();

        for (Invoice inv : overdueInvoices) {
            BigDecimal outstanding = inv.outstandingAmount();
            totalOverdue = totalOverdue.add(outstanding);
            int days = inv.daysOverdue(refDate);
            if (days > maxOverdue) {
                maxOverdue = days;
            }
            items.add(new OverdueInvoiceItemDTO(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.issueDate(),
                    inv.effectiveDueDate(),
                    days,
                    inv.getTotalAmount(),
                    inv.getAmountPaid(),
                    outstanding
            ));
        }

        String noticeRef = String.format("NOT-COB-%s-%04d", refDate.format(DateTimeFormatter.ofPattern("yyyyMM")), client.getId());

        List<String> bankAccounts = List.of(
                "Millennium BIM: 1234567890 (Conta Corrente MZN) - NIB: 000100000123456789012",
                "BCI: 9876543210 (Conta Corrente MZN) - NIB: 000800000987654321098",
                "Standard Bank: 5544332211 (Conta Corrente MZN) - NIB: 000300000554433221144"
        );

        String customMessage = "Solicitamos a regularização das facturas em mora acima discriminadas no prazo impreterível de 5 (cinco) dias úteis "
                + "a contar da data da recepção desta notificação. Após confirmação da transferência, agradecemos o envio do respectivo comprovativo de pagamento.";

        return new DebtCollectionNoticeDTO(
                client.getId(),
                client.getName(),
                client.getTaxId(),
                client.getAddress(),
                client.getEmail(),
                refDate,
                noticeRef,
                totalOverdue,
                maxOverdue,
                items,
                bankAccounts,
                company.getName(),
                company.getTaxId(),
                company.getAddress(),
                company.getPhone(),
                company.getEmail(),
                customMessage
        );
    }

    private static String formatMoney(BigDecimal val) {
        BigDecimal safe = val == null ? BigDecimal.ZERO : val;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Acumulador interno para classificação de risco e cálculo das faixas de aging.
     */
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

            // Determinar nível de risco
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

            // Determinar bloqueio
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

package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.service.ApprovalRequestLookup;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlo e validação de limites de crédito e tramitação de excepções de crédito.
 */
@Service
public class CreditControlService {

    public static final String APPROVAL_DOC_TYPE_CREDIT_EXCEPTION = "CREDIT_EXCEPTION";
    public static final int MAX_ALLOWED_OVERDUE_DAYS = 30;

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ApprovalRequestLookup approvalRequestLookup;
    private final ApprovalService approvalService;

    public CreditControlService(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            ApprovalRequestLookup approvalRequestLookup,
            ApprovalService approvalService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.approvalRequestLookup = approvalRequestLookup;
        this.approvalService = approvalService;
    }

    @Transactional(readOnly = true)
    public void assertCreditAllowed(Client client, BigDecimal newDebt, LocalDate referenceDate) {
        if (client == null) return;
        BigDecimal debtToAdd = newDebt == null ? BigDecimal.ZERO : newDebt;
        if (debtToAdd.signum() <= 0) return;

        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        List<ApprovalRequest> approvedExceptions = approvalRequestLookup.listForDocument(
                companyId, APPROVAL_DOC_TYPE_CREDIT_EXCEPTION, client.getId(), ApprovalStatus.APPROVED);

        if (!approvedExceptions.isEmpty()) {
            return;
        }

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

        if (maxDaysOverdue > MAX_ALLOWED_OVERDUE_DAYS) {
            throw new BusinessRuleException(String.format(
                    "Venda a crédito bloqueada para %s. O cliente possui dívida em mora com atraso de %d dias "
                            + "(limite permitido: %d dias). Requer regularização prévia ou aprovação de excepção pela gerência.",
                    client.getName(), maxDaysOverdue, MAX_ALLOWED_OVERDUE_DAYS));
        }

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

    private static String formatMoney(BigDecimal val) {
        BigDecimal safe = val == null ? BigDecimal.ZERO : val;
        return DisplayNumbers.money(safe);
    }
}

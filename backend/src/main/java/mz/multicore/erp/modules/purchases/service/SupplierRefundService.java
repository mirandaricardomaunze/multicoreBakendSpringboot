package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.SupplierRefundReceivedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Money the supplier pays back after goods already paid for were returned (nota DF). It enters the chosen treasury
 * account and lowers what was paid on the purchase, never below what the purchase is still worth.
 */
@Service
public class SupplierRefundService {

    private final PurchaseRepository purchaseRepository;
    private final FinanceService financeService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final DirectPurchaseQueryService queryService;

    public SupplierRefundService(PurchaseRepository purchaseRepository, @Lazy FinanceService financeService,
                                 ApplicationEventPublisher eventPublisher, AuditLogService auditLogService,
                                 DirectPurchaseQueryService queryService) {
        this.purchaseRepository = purchaseRepository;
        this.financeService = financeService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
    }

    @Transactional
    public PurchaseDTO register(Long purchaseId, BigDecimal amount, Long financeAccountId, String reference) {
        PermissionGuard.requireManagerOrAdmin("registar reembolso de fornecedor");
        if (amount == null || amount.signum() <= 0) throw new BusinessRuleException("O valor do reembolso tem de ser positivo.");
        if (financeAccountId == null) throw new BusinessRuleException("Indique a conta onde o dinheiro entrou.");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .filter(p -> p.getCompany() != null && companyId.equals(p.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException("Fatura de compra não encontrada."));
        PurchaseDTO current = queryService.toDTO(purchase);
        BigDecimal owed = current.supplierOwes();
        if (owed.signum() <= 0) {
            throw new BusinessRuleException("O fornecedor não deve nada nesta compra: só há reembolso depois de devolver mercadoria já paga.");
        }
        if (amount.compareTo(owed) > 0) {
            throw new BusinessRuleException("O fornecedor só deve " + DisplayNumbers.money(owed) + " MT nesta compra.");
        }
        purchase.setAmountPaid(purchase.getAmountPaid().subtract(amount));
        purchaseRepository.save(purchase);

        String description = "Reembolso do fornecedor " + purchase.getSupplier().getName() + " — compra "
                + purchase.getPurchaseNumber() + (reference != null && !reference.isBlank() ? " (ref. " + reference.trim() + ")" : "");
        TreasuryTransactionDTO transaction = financeService.registerTransaction(financeAccountId, "DEBIT", amount, description);
        eventPublisher.publishEvent(new SupplierRefundReceivedEvent(companyId, transaction.id(), purchase.getPurchaseNumber(),
                transaction.transactionDate().toLocalDate(), amount,
                financeService.accountType(financeAccountId) == TreasuryAccountType.CASH));
        auditLogService.logCurrent("SUPPLIER_REFUND", description + ": " + DisplayNumbers.money(amount) + " MT.");
        return queryService.toDTO(purchase);
    }
}

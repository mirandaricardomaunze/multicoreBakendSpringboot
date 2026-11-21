package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.events.SupplierPaymentRegisteredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Supplier payables and their payment through treasury. A payment needs a treasury account and a positive amount
 * within the open balance; cancelled or fully paid purchases accept no payment.
 */
@Service
public class SupplierPaymentService {
    private final PurchaseRepository purchaseRepository;
    private final FinanceService financeService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final DirectPurchaseService directPurchaseService;

    public SupplierPaymentService(PurchaseRepository purchaseRepository,
                                  FinanceService financeService,
                                  ApplicationEventPublisher eventPublisher,
                                  AuditLogService auditLogService,
                                  DirectPurchaseService directPurchaseService) {
        this.purchaseRepository = purchaseRepository;
        this.financeService = financeService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
        this.directPurchaseService = directPurchaseService;
    }

    @Transactional(readOnly = true)
    public List<PayableDTO> findPayablesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return purchaseRepository.findByCompanyId(companyId).stream()
                .filter(p -> !"CANCELLED".equals(p.getStatus()))
                .filter(p -> p.getOutstanding().signum() > 0)
                .map(SupplierPaymentService::toPayable)
                .toList();
    }

    /** Dívida na moeda do documento e o equivalente em moeda base, ao câmbio do documento (SPEC §3.3). */
    private static PayableDTO toPayable(Purchase p) {
        PurchaseBusinessUnit unit = PurchaseBusinessUnit.of(p);
        return new PayableDTO(p.getId(), p.getPurchaseNumber(), p.getSupplier().getId(), p.getSupplier().getName(),
                p.getTotalAmount(), p.getAmountPaid(), p.getOutstanding(), p.getPurchaseDate(),
                unit.id(), unit.name(), p.getCurrency(), p.getExchangeRate(), p.baseTotal(), p.getBaseOutstanding());
    }

    @Transactional
    public PurchaseDTO registerSupplierPayment(Long purchaseId, BigDecimal amount, Long financeAccountId, String reference) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Valor do pagamento deve ser positivo.");
        }
        if (financeAccountId == null) {
            throw new BusinessRuleException("Conta de tesouraria é obrigatória.");
        }
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessRuleException("Fatura de compra não encontrada."));
        CurrentUserContext.requireCompany(purchase.getCompany() != null ? purchase.getCompany().getId() : null);
        if ("CANCELLED".equals(purchase.getStatus())) {
            throw new BusinessRuleException("Compra anulada não tem saldo a pagar.");
        }
        BigDecimal outstanding = purchase.getOutstanding();
        if (outstanding.signum() <= 0) {
            throw new BusinessRuleException("Esta compra já está totalmente paga.");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Valor excede o saldo em dívida (" + DisplayNumbers.money(outstanding) + " MT).");
        }

        purchase.setAmountPaid(purchase.getAmountPaid().add(amount));
        purchase = purchaseRepository.save(purchase);

        String desc = "Pagamento a fornecedor " + purchase.getSupplier().getName()
                + " — " + purchase.paymentLabel()
                + (reference != null && !reference.isBlank() ? " (ref. " + reference + ")" : "");
        TreasuryTransactionDTO transaction = financeService.registerTransaction(
                financeAccountId, "CREDIT", amount, desc);
        eventPublisher.publishEvent(new SupplierPaymentRegisteredEvent(
                purchase.getCompany().getId(), transaction.id(), purchase.getPurchaseNumber(),
                transaction.transactionDate().toLocalDate(), amount,
                financeService.accountType(financeAccountId) == TreasuryAccountType.CASH));
        auditLogService.logCurrent("SUPPLIER_PAYMENT",
                "Compra " + purchase.getPurchaseNumber() + " pagamento " + amount.toPlainString()
                        + " MT (saldo " + purchase.getOutstanding().toPlainString() + " MT)");
        return directPurchaseService.toDTO(purchase);
    }
}

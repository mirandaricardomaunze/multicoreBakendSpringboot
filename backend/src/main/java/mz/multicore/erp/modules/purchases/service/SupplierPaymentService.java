package mz.multicore.erp.modules.purchases.service;

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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
                .map(p -> {
                    Set<String> bus = p.getLines() != null ? p.getLines().stream()
                            .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getName() : null)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet()) : Collections.emptySet();
                    String buName = bus.isEmpty() ? null : (bus.size() == 1 ? bus.iterator().next() : "Misto");
                    Long buId = bus.size() == 1 && p.getLines() != null ? p.getLines().stream()
                            .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getId() : null)
                            .filter(Objects::nonNull).findFirst().orElse(null) : null;
                    return new PayableDTO(
                            p.getId(),
                            p.getPurchaseNumber(),
                            p.getSupplier().getId(),
                            p.getSupplier().getName(),
                            p.getTotalAmount(),
                            p.getAmountPaid(),
                            p.getOutstanding(),
                            p.getPurchaseDate(),
                            buId,
                            buName);
                })
                .toList();
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
            throw new BusinessRuleException("Valor excede o saldo em dívida (" + outstanding.toPlainString() + " MT).");
        }

        purchase.setAmountPaid(purchase.getAmountPaid().add(amount));
        purchase = purchaseRepository.save(purchase);

        String desc = "Pagamento a fornecedor " + purchase.getSupplier().getName()
                + " — Compra " + purchase.getPurchaseNumber()
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

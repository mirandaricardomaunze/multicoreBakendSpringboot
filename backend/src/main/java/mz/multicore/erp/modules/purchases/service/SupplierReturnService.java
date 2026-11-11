package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.SupplierReturnRegisteredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierReturnRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierReturnDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.model.SupplierReturn;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierReturnRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Devolução de mercadoria ao fornecedor (nota DF): a mercadoria sai do armazém da compra, a dívida ao fornecedor
 * baixa pelo valor das linhas devolvidas (preço e IVA da compra) e a contabilidade estorna a entrada
 * ({@code SupplierReturnRegisteredEvent}). Se a compra já estava paga, o fornecedor fica a dever esse valor.
 */
@Service
public class SupplierReturnService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierReturnRepository returnRepository;
    private final InventoryService inventoryService;
    private final DocumentNumberService documentNumberService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    public SupplierReturnService(PurchaseRepository purchaseRepository, SupplierReturnRepository returnRepository,
                                 InventoryService inventoryService, DocumentNumberService documentNumberService,
                                 ApplicationEventPublisher eventPublisher, AuditLogService auditLogService) {
        this.purchaseRepository = purchaseRepository;
        this.returnRepository = returnRepository;
        this.inventoryService = inventoryService;
        this.documentNumberService = documentNumberService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SupplierReturnDTO register(Long purchaseId, CreateSupplierReturnRequest request) {
        PermissionGuard.requireManagerOrAdmin("devolver mercadoria ao fornecedor");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .filter(p -> p.getCompany() != null && companyId.equals(p.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException("Fatura de compra não encontrada."));
        if ("CANCELLED".equals(purchase.getStatus())) {
            throw new BusinessRuleException("Uma compra anulada não pode ter devoluções.");
        }

        SupplierReturn supplierReturn = new SupplierReturn();
        supplierReturn.setCompany(purchase.getCompany());
        supplierReturn.setPurchase(purchase);
        supplierReturn.setReturnDate(LocalDateTime.now());
        supplierReturn.setReason(request.reason().trim());
        supplierReturn.setReturnNumber(documentNumberService.next(DocumentSeries.SUPPLIER_RETURN));
        String description = "Devolução " + supplierReturn.getReturnNumber() + " ao fornecedor "
                + purchase.getSupplier().getName() + " (compra " + purchase.getPurchaseNumber() + ")";

        BigDecimal net = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        Set<Long> seen = new HashSet<>();
        for (CreateSupplierReturnRequest.Line item : request.lines()) {
            if (!seen.add(item.purchaseLineId())) throw new BusinessRuleException("A mesma linha foi indicada mais de uma vez.");
            PurchaseLine line = purchase.getLines().stream().filter(l -> item.purchaseLineId().equals(l.getId())).findFirst()
                    .orElseThrow(() -> new BusinessRuleException("A linha indicada não pertence à compra " + purchase.getPurchaseNumber() + "."));
            BigDecimal returned = line.getReturnedQuantity() == null ? BigDecimal.ZERO : line.getReturnedQuantity();
            BigDecimal available = line.getQuantity().subtract(returned);
            if (item.quantity().compareTo(available) > 0) {
                throw new BusinessRuleException(line.getProduct().getName() + ": só pode devolver "
                        + DisplayNumbers.qty(available) + " (comprado " + DisplayNumbers.qty(line.getQuantity())
                        + ", já devolvido " + DisplayNumbers.qty(returned) + ").");
            }
            inventoryService.registerMovement(line.getProduct(), purchase.getWarehouse(), item.quantity().negate(),
                    "PURCHASE", null, null, description);
            line.setReturnedQuantity(returned.add(item.quantity()));
            LineCalculator.LineAmounts amounts = LineCalculator.compute(line.getUnitPrice(), item.quantity(),
                    BigDecimal.ZERO, line.getTaxRate() == null ? BigDecimal.ZERO : line.getTaxRate());
            net = net.add(amounts.total().subtract(amounts.tax()));
            tax = tax.add(amounts.tax());
        }

        supplierReturn.setNetAmount(net.setScale(2, RoundingMode.HALF_UP));
        supplierReturn.setTaxAmount(tax.setScale(2, RoundingMode.HALF_UP));
        supplierReturn.setTotalAmount(supplierReturn.getNetAmount().add(supplierReturn.getTaxAmount()));
        BigDecimal alreadyReturned = purchase.getReturnedAmount() == null ? BigDecimal.ZERO : purchase.getReturnedAmount();
        purchase.setReturnedAmount(alreadyReturned.add(supplierReturn.getTotalAmount()));
        purchaseRepository.save(purchase);
        SupplierReturn saved = returnRepository.save(supplierReturn);

        eventPublisher.publishEvent(new SupplierReturnRegisteredEvent(companyId, saved.getId(), saved.getReturnNumber(),
                saved.getReturnDate().toLocalDate(), saved.getNetAmount(), saved.getTaxAmount(), saved.getTotalAmount()));
        auditLogService.logCurrent("SUPPLIER_RETURN", description + ": " + DisplayNumbers.money(saved.getTotalAmount())
                + " MT. Motivo: " + saved.getReason());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierReturnDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return returnRepository.findByCompanyIdOrderByReturnDateDesc(companyId).stream().map(this::toDTO).toList();
    }

    private SupplierReturnDTO toDTO(SupplierReturn r) {
        Purchase p = r.getPurchase();
        return new SupplierReturnDTO(r.getId(), r.getReturnNumber(), p.getId(), p.getPurchaseNumber(),
                p.getSupplier() != null ? p.getSupplier().getName() : null, r.getReturnDate(),
                r.getNetAmount(), r.getTaxAmount(), r.getTotalAmount(), r.getReason());
    }
}

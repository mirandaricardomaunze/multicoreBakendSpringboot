package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateLandedCostRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostDocumentDTO;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
import mz.multicore.erp.modules.purchases.model.LandedCostDocument;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.LandedCostDocumentRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Registers "Custos de Importação" documents (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1): numbered in the CI
 * series of the active company and linked only to received purchases of that company.
 */
@Service
public class LandedCostDocumentService {

    private static final String RECEIVED = "COMPLETED";

    private final LandedCostDocumentRepository documentRepository;
    private final PurchaseRepository purchaseRepository;
    private final DocumentNumberService documentNumberService;

    public LandedCostDocumentService(LandedCostDocumentRepository documentRepository,
                                     PurchaseRepository purchaseRepository,
                                     DocumentNumberService documentNumberService) {
        this.documentRepository = documentRepository;
        this.purchaseRepository = purchaseRepository;
        this.documentNumberService = documentNumberService;
    }

    @Transactional
    public LandedCostDocumentDTO register(CreateLandedCostRequest request) {
        PermissionGuard.requireManagerOrAdmin("registar custos de importação");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        List<Purchase> purchases = receivedPurchases(companyId, request.purchaseIds());

        LandedCostDocument document = new LandedCostDocument();
        document.setCompany(purchases.get(0).getCompany());
        document.setDocumentDate(request.documentDate() == null ? LocalDate.now() : request.documentDate());
        document.setAllocationMethod(LandedCostAllocationMethod.orDefault(request.allocationMethod()));
        document.getPurchases().addAll(purchases);
        document.setDocumentNumber(documentNumberService.next(DocumentSeries.LANDED_COST));
        return toDto(documentRepository.save(document));
    }

    private List<Purchase> receivedPurchases(Long companyId, List<Long> purchaseIds) {
        if (purchaseIds == null || purchaseIds.isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos uma compra recebida.");
        }
        return new LinkedHashSet<>(purchaseIds).stream()
                .map(id -> receivedPurchase(companyId, id))
                .toList();
    }

    private Purchase receivedPurchase(Long companyId, Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .filter(p -> p.getCompany() != null && companyId.equals(p.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException("Compra não encontrada na empresa activa."));
        if (!purchase.isPurchase() || !RECEIVED.equals(purchase.getStatus())) {
            throw new BusinessRuleException("A compra " + purchase.getPurchaseNumber()
                    + " não é uma compra recebida: só compras recebidas levam custos de importação.");
        }
        return purchase;
    }

    private static LandedCostDocumentDTO toDto(LandedCostDocument document) {
        return new LandedCostDocumentDTO(document.getId(), document.getDocumentNumber(), document.getDocumentDate(),
                document.getAllocationMethod(),
                document.getPurchases().stream().map(Purchase::getId).toList(),
                document.getPurchases().stream().map(Purchase::getPurchaseNumber).toList());
    }
}

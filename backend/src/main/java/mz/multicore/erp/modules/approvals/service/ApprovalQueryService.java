package mz.multicore.erp.modules.approvals.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.service.StockTransferLookup;
import mz.multicore.erp.modules.inventory.service.StockWasteLookup;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Consultas analíticas e sincronização de documentos pendentes de aprovação ainda não indexados
 * (transferências de stock e quebras de inventário).
 */
@Service
public class ApprovalQueryService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalLifecycleService lifecycleService;
    private final ObjectProvider<StockTransferLookup> stockTransferLookup;
    private final ObjectProvider<StockWasteLookup> stockWasteLookup;

    public ApprovalQueryService(
            ApprovalRequestRepository requestRepository,
            ApprovalLifecycleService lifecycleService,
            ObjectProvider<StockTransferLookup> stockTransferLookup,
            ObjectProvider<StockWasteLookup> stockWasteLookup
    ) {
        this.requestRepository = requestRepository;
        this.lifecycleService = lifecycleService;
        this.stockTransferLookup = stockTransferLookup;
        this.stockWasteLookup = stockWasteLookup;
    }

    @Transactional
    public List<ApprovalRequestDTO> getPendingRequests() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        syncUnindexedPendingDocuments(companyId);
        return requestRepository.findByCompanyIdAndStatus(companyId, ApprovalStatus.PENDING)
                .stream()
                .map(lifecycleService::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestDTO> getAllRequests() {
        return requestRepository.findByCompanyIdOrderByCreatedAtDesc(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(lifecycleService::toDTO)
                .collect(Collectors.toList());
    }

    public void syncUnindexedPendingDocuments(Long companyId) {
        if (companyId == null) return;
        StockTransferLookup transfers = stockTransferLookup == null ? null : stockTransferLookup.getIfAvailable();
        if (transfers != null) {
            for (var transfer : transfers.listForCompany(companyId)) {
                if (transfer.getStatus() != null && transfer.getStatus().isSeparatedOrPending()) {
                    List<ApprovalRequest> existing = requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                            companyId, "STOCK_TRANSFER", transfer.getId(), ApprovalStatus.PENDING);
                    if (existing.isEmpty()) {
                        String originName = transfer.getOriginWarehouse() != null ? transfer.getOriginWarehouse().getName() : "Origem";
                        String destName = transfer.getDestinationWarehouse() != null ? transfer.getDestinationWarehouse().getName() : "Destino";
                        BigDecimal amount = BigDecimal.ZERO;
                        if (transfer.getLines() != null) {
                            for (var line : transfer.getLines()) {
                                BigDecimal price = line.getProduct() != null && line.getProduct().getUnitPrice() != null ? line.getProduct().getUnitPrice() : BigDecimal.ZERO;
                                amount = amount.add(price.multiply(line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO));
                            }
                        }
                        lifecycleService.submitRequest("STOCK_TRANSFER", transfer.getId(), amount,
                                "Transferência " + transfer.getTransferNumber() + " de '" + originName + "' para '" + destName + "'",
                                "MANAGER", false);
                    }
                }
            }
        }
        StockWasteLookup wastes = stockWasteLookup == null ? null : stockWasteLookup.getIfAvailable();
        if (wastes != null) {
            for (var waste : wastes.listForCompanyWithStatus(companyId, WasteStatus.PENDING_APPROVAL)) {
                List<ApprovalRequest> existing = requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                        companyId, "STOCK_WASTE", waste.getId(), ApprovalStatus.PENDING);
                if (existing.isEmpty()) {
                    String prodName = waste.getProduct() != null ? waste.getProduct().getName() : "Artigo";
                    lifecycleService.submitRequest("STOCK_WASTE", waste.getId(), waste.getTotalCost(),
                            "Quebra de stock #" + waste.getId() + " - " + prodName + " (" + waste.getReason() + ")",
                            "MANAGER", false);
                }
            }
        }
    }
}

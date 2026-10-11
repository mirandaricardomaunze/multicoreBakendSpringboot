package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.approvals.service.ApprovalCallback;
import mz.multicore.erp.modules.inventory.dto.ApproveWasteRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Callback do workflow central de aprovações para quebras e desperdícios de stock.
 */
@Component
public class StockWasteApprovalCallback implements ApprovalCallback {

    private final StockWasteService stockWasteService;

    public StockWasteApprovalCallback(@Lazy StockWasteService stockWasteService) {
        this.stockWasteService = stockWasteService;
    }

    @Override
    public boolean supports(String documentType) {
        return "STOCK_WASTE".equalsIgnoreCase(documentType) || "WASTE".equalsIgnoreCase(documentType);
    }

    @Override
    public void onApproved(Long documentId) {
        stockWasteService.approveWaste(documentId, new ApproveWasteRequest(true, "Aprovado via Central de Aprovações."));
    }

    @Override
    public void onRejected(Long documentId, String reason) {
        stockWasteService.approveWaste(documentId, new ApproveWasteRequest(false, reason != null ? reason : "Rejeitado via Central de Aprovações."));
    }
}

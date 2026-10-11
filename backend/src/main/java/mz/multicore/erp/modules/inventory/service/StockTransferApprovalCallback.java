package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.approvals.service.ApprovalCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Callback do workflow central de aprovações para guias de transferência de stock.
 * Permite que um Gestor ou Administrador aprove ou rejeite transferências diretamente
 * no ApprovalsPanel (Central de Aprovações), movimentando o stock via FEFO.
 */
@Component
public class StockTransferApprovalCallback implements ApprovalCallback {

    private final StockTransferService stockTransferService;

    public StockTransferApprovalCallback(@Lazy StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @Override
    public boolean supports(String documentType) {
        return "STOCK_TRANSFER".equalsIgnoreCase(documentType) || "TRANSFER".equalsIgnoreCase(documentType);
    }

    @Override
    public void onApproved(Long documentId) {
        stockTransferService.approveFromWorkflow(documentId);
    }

    @Override
    public void onRejected(Long documentId, String reason) {
        stockTransferService.rejectFromWorkflow(documentId, reason);
    }
}

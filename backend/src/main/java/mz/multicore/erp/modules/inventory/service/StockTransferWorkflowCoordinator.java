package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.StockTransferResolvedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Coordenador de workflow de aprovações, eventos assíncronos e avanço para separação física (picking).
 */
@Component
public class StockTransferWorkflowCoordinator {

    private final ApprovalService approvalService;
    private final ApplicationEventPublisher eventPublisher;
    private final StockTransferRepository transferRepository;
    private final AuditLogService auditLogService;

    public StockTransferWorkflowCoordinator(
            ApprovalService approvalService,
            ApplicationEventPublisher eventPublisher,
            StockTransferRepository transferRepository,
            AuditLogService auditLogService
    ) {
        this.approvalService = approvalService;
        this.eventPublisher = eventPublisher;
        this.transferRepository = transferRepository;
        this.auditLogService = auditLogService;
    }

    public BigDecimal calculateEstimatedAmount(StockTransfer transfer) {
        if (transfer == null || transfer.getLines() == null) return BigDecimal.ZERO;
        return transfer.getLines().stream()
                .map(line -> {
                    BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
                    BigDecimal price = BigDecimal.ZERO;
                    if (line.getProduct() != null) {
                        if (line.getProduct().getUnitPrice() != null && line.getProduct().getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                            price = line.getProduct().getUnitPrice();
                        } else if (line.getProduct().getPurchasePrice() != null) {
                            price = line.getProduct().getPurchasePrice();
                        }
                    }
                    return qty.multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void ensureApprovalRequest(StockTransfer transfer) {
        if (approvalService != null && transfer != null) {
            BigDecimal totalAmount = calculateEstimatedAmount(transfer);
            String originName = transfer.getOriginWarehouse() != null ? transfer.getOriginWarehouse().getName() : "Origem";
            String destName = transfer.getDestinationWarehouse() != null ? transfer.getDestinationWarehouse().getName() : "Destino";
            String desc = "Transferência " + transfer.getTransferNumber() + " de '" + originName + "' para '" + destName + "'";
            approvalService.submitRequest("STOCK_TRANSFER", transfer.getId(), totalAmount, desc, "MANAGER", false);
        }
    }

    public void announce(StockTransfer transfer, StockTransferResolvedEvent.Outcome outcome) {
        if (transfer.getOrderId() == null) return;
        eventPublisher.publishEvent(new StockTransferResolvedEvent(transfer.getOrderId(), outcome));
    }

    public StockTransfer advanceToSeparation(Long id) {
        StockTransfer transfer = transferRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (transfer.getStatus() == TransferStatus.CANCELLED || transfer.getStatus() == TransferStatus.REJECTED) {
            throw new BusinessRuleException("Não é possível emitir guia de separação para uma transferência "
                    + transfer.getStatus().getLabel().toLowerCase() + ".");
        }
        if (transfer.getStatus() == TransferStatus.AWAITING_SEPARATION) {
            transfer.setStatus(TransferStatus.IN_SEPARATION);
            transfer = transferRepository.save(transfer);
            auditLogService.logCurrent("STOCK_TRANSFER_SEPARATION_START",
                    "Guia de separação emitida; separação iniciada para a guia " + transfer.getTransferNumber());
        }
        return transfer;
    }
}

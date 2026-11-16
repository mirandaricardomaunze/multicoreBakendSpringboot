package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.StockTransferLineBatch;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Segundo passo do stock em trânsito: recepção conferida no destino com idempotência, entrada
 * no destino pela ordem FEFO do despacho e devolução da diferença à origem com o mesmo lote.
 * Ver {@code docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md}.
 */
@Service
public class StockTransferReceiptService {

    private final StockTransferRepository transferRepository;
    private final ProductBatchService productBatchService;
    private final StockTransitLedger ledger;
    private final AuditLogService auditLogService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public StockTransferReceiptService(StockTransferRepository transferRepository,
                                       ProductBatchService productBatchService,
                                       StockTransitLedger ledger,
                                       AuditLogService auditLogService,
                                       org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.transferRepository = transferRepository;
        this.productBatchService = productBatchService;
        this.ledger = ledger;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public StockTransferDTO receive(Long id, String notes,
                                    List<ReceiveStockTransferRequest.ReceiveLine> receiptLines,
                                    String idempotencyKey) {
        PermissionGuard.requireManagerOrAdmin("confirmar a recepção de guias de transferência");
        String normalizedKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        String fingerprint = StockTransferReceiptValidator.receiptFingerprint(notes, receiptLines);
        StockTransfer transfer = transferRepository
                .lockByIdAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Transferência não encontrada."));
        if (normalizedKey != null && normalizedKey.equals(transfer.getReceiptIdempotencyKey())) {
            if (!fingerprint.equals(transfer.getReceiptRequestFingerprint())) {
                throw new BusinessRuleException(
                        "A chave de idempotência desta recepção já foi usada com dados diferentes.");
            }
            return StockTransferDtoMapper.toDTO(transfer);
        }
        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BusinessRuleException(
                    "Só guias em trânsito podem ter a recepção confirmada. Estado atual: "
                            + transfer.getStatus().getLabel());
        }

        Map<Long, ReceiveStockTransferRequest.ReceiveLine> declared =
                StockTransferReceiptValidator.indexReceiptLines(transfer, receiptLines);
        Warehouse destination = transfer.getDestinationWarehouse();
        Warehouse origin = transfer.getOriginWarehouse();
        boolean anyDiscrepancy = false;
        StringBuilder discrepancySummary = new StringBuilder();

        for (StockTransferLine line : transfer.getLines()) {
            BigDecimal sent = line.getTransitBatches().stream()
                    .map(StockTransferLineBatch::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ReceiveStockTransferRequest.ReceiveLine req = line.getId() == null ? null : declared.get(line.getId());
            BigDecimal received = req == null ? sent : req.receivedQuantity();
            StockTransferReceiptValidator.validateReceivedQuantity(line, sent, received, req);

            BigDecimal remaining = received;
            BigDecimal enteredDestination = BigDecimal.ZERO;
            BigDecimal returnedOrigin = BigDecimal.ZERO, lostQty = BigDecimal.ZERO;
            boolean lost = req != null && Boolean.TRUE.equals(req.lostInTransit());
            for (StockTransferLineBatch parcel : line.getTransitBatches()) {
                BigDecimal toDestination = parcel.getQuantity().min(remaining);
                BigDecimal toOrigin = parcel.getQuantity().subtract(toDestination);
                remaining = remaining.subtract(toDestination);
                parcel.setReceivedQuantity(toDestination);

                if (toDestination.signum() > 0) {
                    ProductBatch target = productBatchService.addToBatch(line.getProduct(), destination,
                            parcel.getBatchNumber(), parcel.getExpirationDate(), toDestination);
                    ledger.recordMovement(line.getProduct(), destination, toDestination, target, transfer, "entrada");
                    enteredDestination = enteredDestination.add(toDestination);
                }
                if (toOrigin.signum() > 0 && lost) {
                    lostQty = lostQty.add(toOrigin);
                } else if (toOrigin.signum() > 0) {
                    ProductBatch back = productBatchService.addToBatch(line.getProduct(), origin,
                            parcel.getBatchNumber(), parcel.getExpirationDate(), toOrigin);
                    ledger.recordMovement(line.getProduct(), origin, toOrigin, back, transfer,
                            "devolução à origem por divergência");
                    returnedOrigin = returnedOrigin.add(toOrigin);
                }
            }
            if (enteredDestination.signum() > 0) {
                ledger.adjustStock(line.getProduct(), destination, enteredDestination);
            }
            if (returnedOrigin.signum() > 0) {
                ledger.adjustStock(line.getProduct(), origin, returnedOrigin);
                anyDiscrepancy = true;
                if (discrepancySummary.length() > 0) discrepancySummary.append("; ");
                discrepancySummary.append(line.getProduct().getName()).append(": ")
                        .append(returnedOrigin.stripTrailingZeros().toPlainString()).append(" devolvido(s) — ")
                        .append(req.discrepancyReason().trim());
            }
            if (lostQty.signum() > 0) anyDiscrepancy = TransitLoss.record(transfer, line, lostQty, req, eventPublisher, discrepancySummary);
            line.setReceivedQuantity(enteredDestination);
            line.setReturnedQuantity(returnedOrigin);
            line.setDiscrepancyReason(returnedOrigin.add(lostQty).signum() > 0 ? req.discrepancyReason().trim() : null);
        }

        transfer.setStatus(anyDiscrepancy ? TransferStatus.RECEIVED_WITH_DISCREPANCY : TransferStatus.RECEIVED);
        transfer.setReceivedAt(LocalDateTime.now());
        transfer.setReceivedBy(CurrentUserContext.getUsername());
        transfer.setReceiptNotes(notes == null || notes.isBlank() ? null : notes.trim());
        transfer.setReceiptIdempotencyKey(normalizedKey);
        transfer.setReceiptRequestFingerprint(normalizedKey == null ? null : fingerprint);
        StockTransfer saved = transferRepository.save(transfer);
        if (anyDiscrepancy) {
            auditLogService.logCurrent("STOCK_TRANSFER_RECEIVE_DISCREPANCY",
                    "Recepção da guia " + saved.getTransferNumber() + " em '" + destination.getName()
                            + "' com divergência; diferença devolvida a '" + origin.getName() + "': "
                            + discrepancySummary);
        } else {
            auditLogService.logCurrent("STOCK_TRANSFER_RECEIVE",
                    "Recepção da guia " + saved.getTransferNumber() + " confirmada em '" + destination.getName() + "'.");
        }
        return StockTransferDtoMapper.toDTO(saved);
    }
}

package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.InventoryValueChangedEvent;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Goods that never arrived because they broke or disappeared on the way. They already left the origin at dispatch
 * and are not put back there (that would show stock nobody has); the value is written off like a waste,
 * debit CMVMC / credit goods ({@code docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md}).
 */
final class TransitLoss {

    private TransitLoss() {
    }

    /** Records the loss on the line and in the ledger; returns true so the caller flags the discrepancy. */
    static boolean record(StockTransfer transfer, StockTransferLine line, BigDecimal lost,
                          ReceiveStockTransferRequest.ReceiveLine request, ApplicationEventPublisher publisher,
                          StringBuilder summary) {
        line.setLostQuantity(lost);
        if (summary.length() > 0) summary.append("; ");
        summary.append(line.getProduct().getName()).append(": ").append(lost.stripTrailingZeros().toPlainString())
                .append(" perdido(s) no transporte — ").append(request.discrepancyReason().trim());
        BigDecimal cost = line.getProduct().getPurchasePrice() == null ? BigDecimal.ZERO : line.getProduct().getPurchasePrice();
        if (publisher != null && transfer.getCompany() != null && cost.signum() > 0) {
            publisher.publishEvent(new InventoryValueChangedEvent(transfer.getCompany().getId(), JournalSource.TRANSIT_LOSS,
                    line.getId(), transfer.getTransferNumber(), LocalDate.now(), cost.multiply(lost), BigDecimal.ZERO, null,
                    line.getProduct().getId()));
        }
        return true;
    }
}

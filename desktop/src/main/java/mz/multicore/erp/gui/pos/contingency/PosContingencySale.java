package mz.multicore.erp.gui.pos.contingency;

import com.fasterxml.jackson.annotation.JsonFormat;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.PosContingencyStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registo imutável de uma venda efectuada em regime de contingência local no POS.
 */
public record PosContingencySale(
        String contingencyReference,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        POSCheckoutRequest request,
        BigDecimal cartTotal,
        PosContingencyStatus status,
        String syncedInvoiceNumber,
        String errorMessage
) {
    public PosContingencySale withStatus(PosContingencyStatus newStatus, String invoiceNumber, String error) {
        return new PosContingencySale(
                this.contingencyReference,
                this.createdAt,
                this.request,
                this.cartTotal,
                newStatus,
                invoiceNumber != null ? invoiceNumber : this.syncedInvoiceNumber,
                error
        );
    }
}

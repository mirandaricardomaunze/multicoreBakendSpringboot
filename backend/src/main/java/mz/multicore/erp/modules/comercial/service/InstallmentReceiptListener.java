package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.PaymentReceivedEvent;
import mz.multicore.erp.architecture.events.ReceiptCancelledEvent;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps an instalment plan in step with the receipts: every receipt issued or cancelled for the invoice re-spreads what
 * the invoice has paid over its instalments, in order. The plan is a schedule; the money is always the receipt.
 */
@Component
public class InstallmentReceiptListener {

    private final ReceiptRepository receiptRepository;
    private final InstallmentPaymentAmortizer amortizer;

    public InstallmentReceiptListener(ReceiptRepository receiptRepository, InstallmentPaymentAmortizer amortizer) {
        this.receiptRepository = receiptRepository;
        this.amortizer = amortizer;
    }

    @EventListener
    public void onPaymentReceived(PaymentReceivedEvent event) {
        sync(event.receiptId());
    }

    @EventListener
    public void onReceiptCancelled(ReceiptCancelledEvent event) {
        sync(event.receiptId());
    }

    private void sync(Long receiptId) {
        if (receiptId == null) return;
        receiptRepository.findById(receiptId).ifPresent(receipt -> amortizer.syncWithInvoicePayments(
                receipt.getCompany().getId(), receipt.getInvoice().getId(), receipt.getInvoice().getAmountPaid()));
    }
}

package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.PurchaseRegisteredEvent;
import mz.multicore.erp.architecture.events.SupplierPaymentRegisteredEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static mz.multicore.erp.modules.accounting.service.PostingHelper.safe;

/**
 * Escrituração contabilística de compras de mercadorias e pagamentos a fornecedores.
 */
@Service
public class ProcurementPostingHandler {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    public ProcurementPostingHandler(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
    }

    public void postPurchase(PurchaseRegisteredEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.PURCHASE, event.purchaseId())) return;

        BigDecimal total = safe(event.totalAmount());
        if (total.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Compra " + event.purchaseNumber());
        entry.setSource(JournalSource.PURCHASE);
        entry.setSourceDocumentId(event.purchaseId());
        entry.setSourceDocumentNumber(event.purchaseNumber());

        BigDecimal net = safe(event.netAmount());
        BigDecimal tax = safe(event.taxAmount());
        if (net.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.MERCADORIAS),
                    net, "Entrada de mercadorias"));
        }
        if (tax.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.IVA_DEDUTIVEL),
                    tax, "IVA dedutível"));
        }
        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.FORNECEDORES),
                total, "Dívida ao fornecedor"));
        BigDecimal paidNow = safe(event.amountPaidNow());
        if (paidNow.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.FORNECEDORES),
                    paidNow, "Liquidação no acto"));
            entry.addLine(JournalLine.credit(postingHelper.cashAccount(event.companyId(), event.cashPayment()),
                    paidNow, "Pagamento no acto"));
        }
        journalService.save(entry, event.companyId());
    }

    public void postSupplierPayment(SupplierPaymentRegisteredEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.SUPPLIER_PAYMENT, event.transactionId())) return;
        BigDecimal amount = safe(event.amount());
        if (amount.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Pagamento da compra " + event.purchaseNumber());
        entry.setSource(JournalSource.SUPPLIER_PAYMENT);
        entry.setSourceDocumentId(event.transactionId());
        entry.setSourceDocumentNumber(event.purchaseNumber());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.FORNECEDORES),
                amount, "Liquidação ao fornecedor"));
        entry.addLine(JournalLine.credit(postingHelper.cashAccount(event.companyId(), event.cashPayment()),
                amount, "Saída de tesouraria"));
        journalService.save(entry, event.companyId());
    }
}

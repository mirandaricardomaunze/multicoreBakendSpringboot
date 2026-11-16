package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.SupplierRefundReceivedEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Money back from a supplier: debit Caixa or Banco / credit Fornecedores. */
@Service
public class SupplierRefundPostingService {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    public SupplierRefundPostingService(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
    }

    @EventListener
    @Transactional
    public void onSupplierRefund(SupplierRefundReceivedEvent event) {
        BigDecimal amount = PostingHelper.safe(event.amount());
        if (amount.signum() <= 0 || !postingHelper.canPost(event.companyId())
                || postingHelper.alreadyPosted(event.companyId(), JournalSource.SUPPLIER_REFUND, event.transactionId())) return;
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Reembolso do fornecedor — compra " + event.purchaseNumber());
        entry.setSource(JournalSource.SUPPLIER_REFUND);
        entry.setSourceDocumentId(event.transactionId());
        entry.setSourceDocumentNumber(event.purchaseNumber());
        entry.addLine(JournalLine.debit(postingHelper.cashAccount(event.companyId(), event.cash()), amount, "Dinheiro recebido do fornecedor"));
        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.FORNECEDORES), amount,
                "Saldo devedor do fornecedor regularizado"));
        journalService.save(entry, event.companyId());
    }
}

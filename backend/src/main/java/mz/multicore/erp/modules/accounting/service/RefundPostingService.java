package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.CustomerRefundedEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * A POS return refunded in money: the credit note already credited the customer (Clientes), so the money leaving is
 * debit Clientes / credit Caixa or Banco. Without it, the till and the customer account would both stay overstated.
 */
@Service
public class RefundPostingService {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    public RefundPostingService(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
    }

    @EventListener
    @Transactional
    public void onCustomerRefunded(CustomerRefundedEvent event) {
        BigDecimal amount = PostingHelper.safe(event.amount());
        if (amount.signum() <= 0 || !postingHelper.canPost(event.companyId())
                || postingHelper.alreadyPosted(event.companyId(), JournalSource.CUSTOMER_REFUND, event.creditNoteId())) return;
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Reembolso " + event.noteNumber());
        entry.setSource(JournalSource.CUSTOMER_REFUND);
        entry.setSourceDocumentId(event.creditNoteId());
        entry.setSourceDocumentNumber(event.noteNumber());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.CLIENTES), amount,
                "Devolução de dinheiro ao cliente"));
        entry.addLine(JournalLine.credit(postingHelper.cashAccount(event.companyId(), event.cash()), amount, "Reembolso pago"));
        journalService.save(entry, event.companyId());
    }
}

package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.TreasuryPayoutEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Automatic payouts that had no posting: bonuses and final settlements (6301), staff advances and loans (2602, cleared
 * by the payslip deductions), reimbursed expenses and outside services (6201); credit Caixa or Banco.
 */
@Service
public class TreasuryPayoutPostingService {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    public TreasuryPayoutPostingService(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
    }

    @EventListener
    @Transactional
    public void onPayout(TreasuryPayoutEvent event) {
        BigDecimal amount = PostingHelper.safe(event.amount());
        String debit = switch (event.purpose()) {
            case STAFF_COST -> PgcNirfChart.CUSTOS_PESSOAL;
            case STAFF_ADVANCE -> PgcNirfChart.PESSOAL_OUTROS_DESCONTOS;
            case STAFF_EXPENSE, EXTERNAL_SERVICE -> PgcNirfChart.FSE;
        };
        if (amount.signum() <= 0 || event.transactionId() == null || !postingHelper.canPost(event.companyId())
                || !postingHelper.hasAccounts(event.companyId(), debit)
                || postingHelper.alreadyPosted(event.companyId(), JournalSource.TREASURY_PAYOUT, event.transactionId())) return;
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        String text = event.description() == null ? "Pagamento automático" : event.description();
        entry.setDescription(text.length() > 250 ? text.substring(0, 250) : text);
        entry.setSource(JournalSource.TREASURY_PAYOUT);
        entry.setSourceDocumentId(event.transactionId());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), debit), amount, "Pagamento"));
        entry.addLine(JournalLine.credit(postingHelper.cashAccount(event.companyId(), event.cash()), amount, "Saída de tesouraria"));
        journalService.save(entry, event.companyId());
    }
}

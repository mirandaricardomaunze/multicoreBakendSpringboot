package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.InventoryValueChangedEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Posts stock value changes that are neither sales nor purchases (wastes, adjustments, breakdowns, production), so
 * the goods account (3201) follows the physical stock. Value out: debit CMVMC (6101), credit goods. Value in: debit
 * goods, credit the counterpart of the event. Skipped when the company has no chart or already posted that document.
 */
@Service
public class InventoryPostingService {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    private final GoodsAccounts goodsAccounts;

    public InventoryPostingService(PostingHelper postingHelper, JournalService journalService, GoodsAccounts goodsAccounts) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
        this.goodsAccounts = goodsAccounts;
    }

    @EventListener
    @Transactional
    public void onInventoryValueChanged(InventoryValueChangedEvent event) {
        BigDecimal out = scale(event.valueOut());
        BigDecimal in = scale(event.valueIn());
        if (out.signum() == 0 && in.signum() == 0) return;
        String counterpart = event.inCounterpart() == null ? PgcNirfChart.CMVMC : event.inCounterpart();
        if (!postingHelper.canPost(event.companyId())
                || !postingHelper.hasAccounts(event.companyId(), PgcNirfChart.MERCADORIAS, PgcNirfChart.CMVMC, counterpart)) return;
        if (event.sourceId() != null && postingHelper.alreadyPosted(event.companyId(), event.source(), event.sourceId())) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date() != null ? event.date() : LocalDate.now());
        entry.setDescription(event.source().label() + (event.reference() == null ? "" : " " + event.reference()));
        entry.setSource(event.source());
        entry.setSourceDocumentId(event.sourceId());
        entry.setSourceDocumentNumber(event.reference() == null ? null
                : event.reference().substring(0, Math.min(40, event.reference().length())));
        // A production batch consumes bought ingredients (3201); a waste or adjustment stays in the product's account.
        String inAccount = goodsAccounts.accountFor(event.companyId(), event.productId());
        String outAccount = event.source() == JournalSource.PRODUCTION_RUN ? PgcNirfChart.MERCADORIAS : inAccount;
        if (out.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.CMVMC), out, "Saída de existências"));
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), outAccount), out, "Saída de existências"));
        }
        if (in.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), inAccount), in, "Entrada de existências"));
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), counterpart), in, "Entrada de existências"));
        }
        journalService.save(entry, event.companyId());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.abs().setScale(2, RoundingMode.HALF_UP);
    }
}

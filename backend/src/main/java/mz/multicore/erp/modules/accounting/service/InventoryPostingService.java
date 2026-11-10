package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.InventoryValueChangedEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
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

    public InventoryPostingService(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
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
        if (out.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.CMVMC), out, "Saída de existências"));
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.MERCADORIAS), out, "Saída de existências"));
        }
        if (in.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.MERCADORIAS), in, "Entrada de existências"));
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), counterpart), in, "Entrada de existências"));
        }
        journalService.save(entry, event.companyId());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.abs().setScale(2, RoundingMode.HALF_UP);
    }
}

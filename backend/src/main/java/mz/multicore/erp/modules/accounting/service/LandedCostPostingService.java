package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.LandedCostConfirmedEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static mz.multicore.erp.modules.accounting.service.PostingHelper.safe;

/**
 * Lançamento do documento "Custos de Importação" (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.4), em moeda base:
 * débito 3201 pela parte do stock, 6101 pela parte já vendida e 2432 pelo IVA; crédito 2201 pela dívida de cada
 * linha ao seu fornecedor.
 */
@Service
public class LandedCostPostingService {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    public LandedCostPostingService(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
    }

    @EventListener
    @Transactional
    public void onLandedCostConfirmed(LandedCostConfirmedEvent event) {
        Long companyId = event.companyId();
        if (event.payables().isEmpty() || !postingHelper.canPost(companyId)
                || postingHelper.alreadyPosted(companyId, JournalSource.LANDED_COST, event.documentId())) return;
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Custos de Importação " + event.documentNumber());
        entry.setSource(JournalSource.LANDED_COST);
        entry.setSourceDocumentId(event.documentId());
        entry.setSourceDocumentNumber(event.documentNumber());
        debit(entry, companyId, PgcNirfChart.MERCADORIAS, event.stockAmount(), "Custos de importação no stock");
        debit(entry, companyId, PgcNirfChart.CMVMC, event.soldAmount(), "Custos de importação da parte já vendida");
        debit(entry, companyId, PgcNirfChart.IVA_DEDUTIVEL, event.vatAmount(), "IVA dedutível da importação");
        for (LandedCostConfirmedEvent.Payable payable : event.payables()) {
            BigDecimal amount = safe(payable.amount());
            if (amount.signum() > 0) {
                entry.addLine(JournalLine.credit(postingHelper.account(companyId, PgcNirfChart.FORNECEDORES), amount,
                        "Dívida " + payable.number() + " · " + payable.supplierName()));
            }
        }
        journalService.save(entry, companyId);
    }

    private void debit(JournalEntry entry, Long companyId, String accountCode, BigDecimal amount, String description) {
        if (safe(amount).signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(companyId, accountCode), amount, description));
        }
    }
}

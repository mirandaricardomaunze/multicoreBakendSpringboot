package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.SupplierReturnRegisteredEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Goods returned to the supplier reverse the purchase: debit Fornecedores / credit Mercadorias and IVA dedutível. */
@Service
public class SupplierReturnPostingService {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    public SupplierReturnPostingService(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
    }

    @EventListener
    @Transactional
    public void onSupplierReturn(SupplierReturnRegisteredEvent event) {
        BigDecimal total = PostingHelper.safe(event.totalAmount());
        if (total.signum() <= 0 || !postingHelper.canPost(event.companyId())
                || postingHelper.alreadyPosted(event.companyId(), JournalSource.SUPPLIER_RETURN, event.returnId())) return;
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Devolução ao fornecedor " + event.returnNumber());
        entry.setSource(JournalSource.SUPPLIER_RETURN);
        entry.setSourceDocumentId(event.returnId());
        entry.setSourceDocumentNumber(event.returnNumber());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.FORNECEDORES), total,
                "Redução da dívida ao fornecedor"));
        BigDecimal net = PostingHelper.safe(event.netAmount());
        BigDecimal tax = PostingHelper.safe(event.taxAmount());
        if (net.signum() > 0) {
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.MERCADORIAS), net,
                    "Saída de mercadorias devolvidas"));
        }
        if (tax.signum() > 0) {
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.IVA_DEDUTIVEL), tax,
                    "Regularização do IVA dedutível"));
        }
        journalService.save(entry, event.companyId());
    }
}

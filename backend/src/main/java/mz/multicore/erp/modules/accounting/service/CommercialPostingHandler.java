package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.CreditNoteApprovedEvent;
import mz.multicore.erp.architecture.events.DebitNoteApprovedEvent;
import mz.multicore.erp.architecture.events.InvoiceCancelledEvent;
import mz.multicore.erp.architecture.events.PaymentReceivedEvent;
import mz.multicore.erp.architecture.events.ReceiptCancelledEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static mz.multicore.erp.modules.accounting.service.PostingHelper.safe;

/**
 * Escrituração contabilística de vendas, recebimentos e notas de crédito/débito.
 */
@Service
public class CommercialPostingHandler {

    private final PostingHelper postingHelper;
    private final JournalService journalService;

    public CommercialPostingHandler(PostingHelper postingHelper, JournalService journalService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
    }

    public void postSale(SaleRegisteredEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.INVOICE, event.invoiceId())) return;

        BigDecimal total = safe(event.totalAmount());
        BigDecimal net = safe(event.netAmount());
        BigDecimal tax = safe(event.taxAmount());
        BigDecimal paidNow = safe(event.amountPaidNow());
        if (total.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Venda " + event.invoiceNumber());
        entry.setSource(JournalSource.INVOICE);
        entry.setSourceDocumentId(event.invoiceId());
        entry.setSourceDocumentNumber(event.invoiceNumber());

        BigDecimal onCredit = total.subtract(paidNow);
        if (paidNow.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.cashAccount(event.companyId(), event.cashPayment()),
                    paidNow, "Recebido no acto"));
        }
        if (onCredit.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.CLIENTES),
                    onCredit, "A receber do cliente"));
        }

        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.VENDAS),
                net, "Vendas de mercadorias"));
        if (tax.signum() > 0) {
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.IVA_LIQUIDADO),
                    tax, "IVA liquidado"));
        }

        BigDecimal cost = safe(event.costOfGoods());
        if (cost.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.CMVMC),
                    cost, "Custo das mercadorias vendidas"));
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.MERCADORIAS),
                    cost, "Saída de existências"));
        }

        journalService.save(entry, event.companyId());
    }

    public void postPayment(PaymentReceivedEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.RECEIPT, event.receiptId())) return;

        BigDecimal amount = safe(event.amount());
        if (amount.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Recebimento " + event.receiptNumber());
        entry.setSource(JournalSource.RECEIPT);
        entry.setSourceDocumentId(event.receiptId());
        entry.setSourceDocumentNumber(event.receiptNumber());
        entry.addLine(JournalLine.debit(postingHelper.cashAccount(event.companyId(), event.cashPayment()),
                amount, "Recebimento de cliente"));
        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.CLIENTES),
                amount, "Liquidação de conta corrente"));

        journalService.save(entry, event.companyId());
    }

    public void postInvoiceCancellation(InvoiceCancelledEvent event) {
        postReversal(event.companyId(), JournalSource.INVOICE, JournalSource.INVOICE_CANCELLATION,
                event.invoiceId(), event.invoiceNumber(), event.date(), "Anulação da venda ");
    }

    public void postReceiptCancellation(ReceiptCancelledEvent event) {
        postReversal(event.companyId(), JournalSource.RECEIPT, JournalSource.RECEIPT_CANCELLATION,
                event.receiptId(), event.receiptNumber(), event.date(), "Anulação do recebimento ");
    }

    /** Estorna um lançamento trocando débitos e créditos; o original fica no diário. */
    private void postReversal(Long companyId, JournalSource original, JournalSource reversal,
                              Long documentId, String documentNumber, LocalDate date, String description) {
        if (!postingHelper.canPost(companyId)) return;
        if (postingHelper.alreadyPosted(companyId, reversal, documentId)) return;
        journalService.findByDocument(companyId, original, documentId).ifPresent(posted -> {
            JournalEntry entry = new JournalEntry();
            entry.setEntryDate(date);
            entry.setDescription(description + documentNumber);
            entry.setSource(reversal);
            entry.setSourceDocumentId(documentId);
            entry.setSourceDocumentNumber(documentNumber);
            for (JournalLine line : posted.getLines()) {
                entry.addLine(safe(line.getDebit()).signum() > 0
                        ? JournalLine.credit(line.getAccount(), line.getDebit(), "Estorno: " + line.getDescription())
                        : JournalLine.debit(line.getAccount(), line.getCredit(), "Estorno: " + line.getDescription()));
            }
            journalService.save(entry, companyId);
        });
    }

    public void postCreditNote(CreditNoteApprovedEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.CREDIT_NOTE, event.noteId())) return;
        BigDecimal total = safe(event.totalAmount());
        if (total.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Nota de crédito " + event.noteNumber());
        entry.setSource(JournalSource.CREDIT_NOTE);
        entry.setSourceDocumentId(event.noteId());
        entry.setSourceDocumentNumber(event.noteNumber());
        BigDecimal net = safe(event.netAmount());
        BigDecimal tax = safe(event.taxAmount());
        if (net.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.VENDAS),
                    net, "Estorno da venda"));
        }
        if (tax.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.IVA_LIQUIDADO),
                    tax, "Estorno do IVA liquidado"));
        }
        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.CLIENTES),
                total, "Redução da dívida do cliente"));
        BigDecimal cost = safe(event.returnedGoodsCost());
        if (cost.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.MERCADORIAS),
                    cost, "Reposição das mercadorias devolvidas"));
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.CMVMC),
                    cost, "Estorno do custo da venda"));
        }
        journalService.save(entry, event.companyId());
    }

    public void postDebitNote(DebitNoteApprovedEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.DEBIT_NOTE, event.noteId())) return;
        BigDecimal total = safe(event.totalAmount());
        if (total.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Nota de débito " + event.noteNumber());
        entry.setSource(JournalSource.DEBIT_NOTE);
        entry.setSourceDocumentId(event.noteId());
        entry.setSourceDocumentNumber(event.noteNumber());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.CLIENTES),
                total, "Acréscimo à dívida do cliente"));
        BigDecimal net = safe(event.netAmount());
        BigDecimal tax = safe(event.taxAmount());
        if (net.signum() > 0) {
            entry.addLine(JournalLine.credit(
                    postingHelper.account(event.companyId(), PgcNirfChart.OUTROS_PROVEITOS_OPERACIONAIS),
                    net, "Frete, sobretaxa ou correcção"));
        }
        if (tax.signum() > 0) {
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.IVA_LIQUIDADO),
                    tax, "IVA liquidado"));
        }
        journalService.save(entry, event.companyId());
    }
}

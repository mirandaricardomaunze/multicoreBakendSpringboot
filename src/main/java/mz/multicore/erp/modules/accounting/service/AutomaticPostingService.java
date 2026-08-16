package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.PaymentReceivedEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Traduz factos de negócio em partidas dobradas.
 *
 * <p>Ouve eventos ({@code @EventListener} síncrono, <b>na mesma transacção</b> da operação de
 * origem): se o lançamento falhar, a venda não fica gravada pela metade. O módulo comercial não
 * conhece a contabilidade — só publica o que aconteceu.
 *
 * <p><b>Sem plano de contas, não lança e não estoira.</b> Uma empresa que ainda não semeou o
 * plano tem de poder continuar a vender; a contabilidade é opcional até alguém a ligar.
 */
@Service
public class AutomaticPostingService {

    private final ChartOfAccountsService chartOfAccountsService;
    private final JournalService journalService;

    public AutomaticPostingService(ChartOfAccountsService chartOfAccountsService,
                                   JournalService journalService) {
        this.chartOfAccountsService = chartOfAccountsService;
        this.journalService = journalService;
    }

    /**
     * Venda:
     * <pre>
     *   D  Clientes / Caixa      total
     *   C  Vendas                líquido
     *   C  IVA liquidado         imposto
     *   D  CMVMC                 custo        (só se o custo for conhecido)
     *   C  Mercadorias           custo
     * </pre>
     * Uma venda paga na hora entra directamente em Caixa/Banco; a fiado fica em Clientes, e é o
     * {@link #onPaymentReceived} que a transfere mais tarde.
     */
    @EventListener
    @Transactional
    public void onSaleRegistered(SaleRegisteredEvent event) {
        if (!canPost(event.companyId())) return;
        if (alreadyPosted(event.companyId(), JournalSource.INVOICE, event.invoiceId())) return;

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

        // Débito: o que entrou agora vai a Caixa/Banco; o resto fica por receber em Clientes.
        BigDecimal onCredit = total.subtract(paidNow);
        if (paidNow.signum() > 0) {
            entry.addLine(JournalLine.debit(cashAccount(event.companyId(), event.cashPayment()),
                    paidNow, "Recebido no acto"));
        }
        if (onCredit.signum() > 0) {
            entry.addLine(JournalLine.debit(account(event.companyId(), PgcNirfChart.CLIENTES),
                    onCredit, "A receber do cliente"));
        }

        entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.VENDAS),
                net, "Vendas de mercadorias"));
        if (tax.signum() > 0) {
            entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.IVA_LIQUIDADO),
                    tax, "IVA liquidado"));
        }

        // Custo das mercadorias vendidas: só quando se sabe o custo. Inventar um custo para o
        // lançamento fechar seria pior do que não lançar a margem.
        BigDecimal cost = safe(event.costOfGoods());
        if (cost.signum() > 0) {
            entry.addLine(JournalLine.debit(account(event.companyId(), PgcNirfChart.CMVMC),
                    cost, "Custo das mercadorias vendidas"));
            entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.MERCADORIAS),
                    cost, "Saída de existências"));
        }

        journalService.save(entry, event.companyId());
    }

    /**
     * Recebimento:
     * <pre>
     *   D  Caixa / Banco     valor
     *   C  Clientes          valor
     * </pre>
     * Só movimenta o saldo — o proveito já foi lançado na emissão da fatura. Lançar Vendas outra
     * vez aqui contaria a mesma venda duas vezes, que é o erro clássico deste tipo de integração.
     */
    @EventListener
    @Transactional
    public void onPaymentReceived(PaymentReceivedEvent event) {
        if (!canPost(event.companyId())) return;
        if (alreadyPosted(event.companyId(), JournalSource.RECEIPT, event.receiptId())) return;

        BigDecimal amount = safe(event.amount());
        if (amount.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Recebimento " + event.receiptNumber());
        entry.setSource(JournalSource.RECEIPT);
        entry.setSourceDocumentId(event.receiptId());
        entry.setSourceDocumentNumber(event.receiptNumber());
        entry.addLine(JournalLine.debit(cashAccount(event.companyId(), event.cashPayment()),
                amount, "Recebimento de cliente"));
        entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.CLIENTES),
                amount, "Liquidação de conta corrente"));

        journalService.save(entry, event.companyId());
    }

    private boolean canPost(Long companyId) {
        return companyId != null && chartOfAccountsService.hasChart(companyId);
    }

    private boolean alreadyPosted(Long companyId, JournalSource source, Long documentId) {
        return documentId != null
                && journalService.findByDocument(companyId, source, documentId).isPresent();
    }

    private Account cashAccount(Long companyId, boolean cash) {
        return account(companyId, cash ? PgcNirfChart.CAIXA : PgcNirfChart.BANCO);
    }

    private Account account(Long companyId, String code) {
        return chartOfAccountsService.requirePostableAccount(companyId, code);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

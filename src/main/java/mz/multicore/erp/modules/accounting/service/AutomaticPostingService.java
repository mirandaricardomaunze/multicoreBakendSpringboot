package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.PayrollLiabilityDeliveredEvent;
import mz.multicore.erp.architecture.events.PaymentReceivedEvent;
import mz.multicore.erp.architecture.events.PayslipPaidEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.audit.service.AuditLogService;

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
    private final AuditLogService auditLogService;

    public AutomaticPostingService(ChartOfAccountsService chartOfAccountsService,
                                   JournalService journalService,
                                   AuditLogService auditLogService) {
        this.chartOfAccountsService = chartOfAccountsService;
        this.journalService = journalService;
        this.auditLogService = auditLogService;
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

    /**
     * Salários pagos:
     * <pre>
     *   D  Custos com o pessoal          ilíquido − faltas descontadas
     *   D  Encargos sobre remunerações   INSS patronal
     *   C  IRPS retido a entregar        IRPS
     *   C  INSS a entregar               quota do trabalhador + quota patronal
     *   C  Outros descontos ao pessoal   outros descontos      (só se existirem)
     *   C  Caixa                         líquido pago
     * </pre>
     * <b>O maior custo fixo de uma empresa de retalho não chegava ao razão</b> — declarado em falta
     * na CONTABILIDADE_SPEC §7. O desconto por faltas entra a reduzir o custo (e não como proveito)
     * porque é trabalho que não foi prestado, não é receita da empresa.
     *
     * <p>O INSS patronal é <b>custo</b>, não retenção ao trabalhador; por isso é débito de conta
     * própria e não sai do ilíquido. Tê-lo diluído no mesmo sítio das retenções foi sempre a forma
     * de ele desaparecer.
     */
    @EventListener
    @Transactional
    public void onPayslipPaid(PayslipPaidEvent event) {
        if (!canPost(event.companyId())) return;
        if (!hasPayrollAccounts(event.companyId())) {
            auditLogService.logEvent("SYSTEM", event.companyId(), "PAYROLL_POSTING_SKIPPED",
                    String.format("Recibo %s não foi lançado na contabilidade: o plano de contas "
                            + "desta empresa não tem as contas de pessoal. Semeie o plano de contas "
                            + "em Contabilidade para as acrescentar.", event.payslipNumber()));
            return;
        }
        if (alreadyPosted(event.companyId(), JournalSource.PAYROLL, event.payslipId())) return;

        BigDecimal cost = safe(event.grossPay()).subtract(safe(event.absenceDeduction()));
        if (cost.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Salário " + event.payslipNumber() + " — " + event.employeeName());
        entry.setSource(JournalSource.PAYROLL);
        entry.setSourceDocumentId(event.payslipId());
        entry.setSourceDocumentNumber(event.payslipNumber());

        entry.addLine(JournalLine.debit(account(event.companyId(), PgcNirfChart.CUSTOS_PESSOAL),
                cost, "Remunerações do mês"));
        BigDecimal employerInss = safe(event.employerInss());
        if (employerInss.signum() > 0) {
            entry.addLine(JournalLine.debit(account(event.companyId(), PgcNirfChart.ENCARGOS_PESSOAL),
                    employerInss, "INSS a cargo da empresa"));
        }

        BigDecimal irps = safe(event.irps());
        if (irps.signum() > 0) {
            entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.IRPS_RETIDO),
                    irps, "IRPS retido a entregar"));
        }
        BigDecimal inss = safe(event.employeeInss()).add(employerInss);
        if (inss.signum() > 0) {
            entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.INSS_A_ENTREGAR),
                    inss, "INSS a entregar"));
        }
        BigDecimal other = safe(event.otherDeductions());
        if (other.signum() > 0) {
            entry.addLine(JournalLine.credit(
                    account(event.companyId(), PgcNirfChart.PESSOAL_OUTROS_DESCONTOS),
                    other, "Outros descontos ao pessoal"));
        }
        entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.CAIXA),
                safe(event.netPay()), "Líquido pago ao colaborador"));

        journalService.save(entry, event.companyId());
    }

    /**
     * Entrega das retenções ao Estado:
     * <pre>
     *   D  IRPS retido / INSS a entregar   valor
     *   C  Caixa                           valor
     * </pre>
     * Fecha o par do {@link #onPayslipPaid}: sem isto a conta de retenções crescia para sempre, o
     * dinheiro saía da caixa e a dívida ficava lá — o balancete deixava de bater com a realidade.
     */
    @EventListener
    @Transactional
    public void onPayrollLiabilityDelivered(PayrollLiabilityDeliveredEvent event) {
        if (!canPost(event.companyId())) return;
        if (!hasPayrollAccounts(event.companyId())) {
            auditLogService.logEvent("SYSTEM", event.companyId(), "PAYROLL_POSTING_SKIPPED",
                    String.format("A entrega de %s de %d/%d não foi lançada na contabilidade: o "
                                    + "plano de contas não tem as contas de pessoal.",
                            event.liabilityType(), event.month(), event.year()));
            return;
        }
        if (alreadyPosted(event.companyId(), JournalSource.PAYROLL_DELIVERY, event.liabilityId())) return;

        BigDecimal amount = safe(event.amount());
        if (amount.signum() <= 0) return;

        String liabilityAccount = "IRPS".equals(event.liabilityType())
                ? PgcNirfChart.IRPS_RETIDO : PgcNirfChart.INSS_A_ENTREGAR;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription(String.format("Entrega %s %d/%d",
                event.liabilityType(), event.month(), event.year()));
        entry.setSource(JournalSource.PAYROLL_DELIVERY);
        entry.setSourceDocumentId(event.liabilityId());
        entry.setSourceDocumentNumber(event.paymentReference());
        entry.addLine(JournalLine.debit(account(event.companyId(), liabilityAccount),
                amount, "Retenção entregue ao Estado"));
        entry.addLine(JournalLine.credit(account(event.companyId(), PgcNirfChart.CAIXA),
                amount, "Pagamento ao Estado"));

        journalService.save(entry, event.companyId());
    }

    private boolean canPost(Long companyId) {
        return companyId != null && chartOfAccountsService.hasChart(companyId);
    }

    /**
     * O plano desta empresa já tem as contas de pessoal.
     *
     * <p>Uma empresa que semeou o plano antes destas contas existirem tem um plano <b>incompleto</b>,
     * não inexistente. Recusar o pagamento do salário por causa disso seria pior do que o problema:
     * o salário está certo, o que falta é escriturá-lo. Por isso não se lança, <b>fica rasto na
     * auditoria</b> a dizer o que fazer, e o utilizador resolve semeando o plano outra vez — que
     * agora preenche só as lacunas.
     */
    private boolean hasPayrollAccounts(Long companyId) {
        return chartOfAccountsService.hasAccounts(companyId,
                PgcNirfChart.CUSTOS_PESSOAL, PgcNirfChart.ENCARGOS_PESSOAL,
                PgcNirfChart.IRPS_RETIDO, PgcNirfChart.INSS_A_ENTREGAR,
                PgcNirfChart.PESSOAL_OUTROS_DESCONTOS, PgcNirfChart.CAIXA);
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

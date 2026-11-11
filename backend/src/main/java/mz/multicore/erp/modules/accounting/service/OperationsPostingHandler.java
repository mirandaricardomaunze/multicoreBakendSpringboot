package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.AssetDepreciatedEvent;
import mz.multicore.erp.architecture.events.FleetFuelLoggedEvent;
import mz.multicore.erp.architecture.events.PayrollLiabilityDeliveredEvent;
import mz.multicore.erp.architecture.events.PayslipPaidEvent;
import mz.multicore.erp.architecture.events.ProductionCompletedEvent;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static mz.multicore.erp.modules.accounting.service.PostingHelper.safe;

/**
 * Escrituração contabilística de recursos humanos (salários e guias fiscais), produção industrial,
 * depreciação de activos e consumo de combustível da frota.
 */
@Service
public class OperationsPostingHandler {

    private final PostingHelper postingHelper;
    private final JournalService journalService;
    private final AuditLogService auditLogService;

    public OperationsPostingHandler(PostingHelper postingHelper,
                                    JournalService journalService,
                                    AuditLogService auditLogService) {
        this.postingHelper = postingHelper;
        this.journalService = journalService;
        this.auditLogService = auditLogService;
    }

    public void postPayslip(PayslipPaidEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (!postingHelper.hasPayrollAccounts(event.companyId())) {
            auditLogService.logEvent("SYSTEM", event.companyId(), "PAYROLL_POSTING_SKIPPED",
                    String.format("Recibo %s não foi lançado na contabilidade: o plano de contas "
                            + "desta empresa não tem as contas de pessoal. Semeie o plano de contas "
                            + "em Contabilidade para as acrescentar.", event.payslipNumber()));
            return;
        }
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.PAYROLL, event.payslipId())) return;

        BigDecimal cost = safe(event.grossPay()).subtract(safe(event.absenceDeduction()));
        if (cost.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date());
        entry.setDescription("Salário " + event.payslipNumber() + " — " + event.employeeName());
        entry.setSource(JournalSource.PAYROLL);
        entry.setSourceDocumentId(event.payslipId());
        entry.setSourceDocumentNumber(event.payslipNumber());

        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.CUSTOS_PESSOAL),
                cost, "Remunerações do mês"));
        BigDecimal employerInss = safe(event.employerInss());
        if (employerInss.signum() > 0) {
            entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.ENCARGOS_PESSOAL),
                    employerInss, "INSS a cargo da empresa"));
        }

        BigDecimal irps = safe(event.irps());
        if (irps.signum() > 0) {
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.IRPS_RETIDO),
                    irps, "IRPS retido a entregar"));
        }
        BigDecimal inss = safe(event.employeeInss()).add(employerInss);
        if (inss.signum() > 0) {
            entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.INSS_A_ENTREGAR),
                    inss, "INSS a entregar"));
        }
        BigDecimal other = safe(event.otherDeductions());
        if (other.signum() > 0) {
            entry.addLine(JournalLine.credit(
                    postingHelper.account(event.companyId(), PgcNirfChart.PESSOAL_OUTROS_DESCONTOS),
                    other, "Outros descontos ao pessoal"));
        }
        entry.addLine(JournalLine.credit(postingHelper.cashAccount(event.companyId(), event.cashPayment()),
                safe(event.netPay()), "Líquido pago ao colaborador"));

        journalService.save(entry, event.companyId());
    }

    public void postPayrollLiability(PayrollLiabilityDeliveredEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (!postingHelper.hasPayrollAccounts(event.companyId())) {
            auditLogService.logEvent("SYSTEM", event.companyId(), "PAYROLL_POSTING_SKIPPED",
                    String.format("A entrega de %s de %d/%d não foi lançada na contabilidade: o "
                                    + "plano de contas não tem as contas de pessoal.",
                            event.liabilityType(), event.month(), event.year()));
            return;
        }
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.PAYROLL_DELIVERY, event.liabilityId())) return;

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
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), liabilityAccount),
                amount, "Retenção entregue ao Estado"));
        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.CAIXA),
                amount, "Pagamento ao Estado"));

        journalService.save(entry, event.companyId());
    }

    public void postProduction(ProductionCompletedEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (!postingHelper.hasAccounts(event.companyId(), PgcNirfChart.PRODUTOS_ACABADOS, PgcNirfChart.VARIACAO_PRODUCAO)) {
            return;
        }
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.PRODUCTION, event.productionOrderId())) return;

        BigDecimal cip = safe(event.totalCipCost());
        if (cip.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date() != null ? event.date() : LocalDate.now());
        entry.setDescription("Conclusão de Produção " + event.orderNumber());
        entry.setSource(JournalSource.PRODUCTION);
        entry.setSourceDocumentId(event.productionOrderId());
        entry.setSourceDocumentNumber(event.orderNumber());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.PRODUTOS_ACABADOS),
                cip, "Entrada de produto acabado no armazém"));
        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.VARIACAO_PRODUCAO),
                cip, "Apuramento da variação de produção"));

        journalService.save(entry, event.companyId());
    }

    public void postAssetDepreciation(AssetDepreciatedEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (!postingHelper.hasAccounts(event.companyId(), PgcNirfChart.DEPRECIACAO_EXERCICIO, PgcNirfChart.AMORTIZACOES_ACUMULADAS)) {
            return;
        }
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.ASSET_DEPRECIATION, event.assetId())) return;

        BigDecimal amount = safe(event.depreciationAmount());
        if (amount.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date() != null ? event.date() : LocalDate.now());
        entry.setDescription("Amortização do Activo " + event.assetCode());
        entry.setSource(JournalSource.ASSET_DEPRECIATION);
        entry.setSourceDocumentId(event.assetId());
        entry.setSourceDocumentNumber(event.assetCode());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.DEPRECIACAO_EXERCICIO),
                amount, "Gasto com amortizações do exercício"));
        entry.addLine(JournalLine.credit(postingHelper.account(event.companyId(), PgcNirfChart.AMORTIZACOES_ACUMULADAS),
                amount, "Quota de amortização acumulada"));

        journalService.save(entry, event.companyId());
    }

    public void postFleetFuel(FleetFuelLoggedEvent event) {
        if (!postingHelper.canPost(event.companyId())) return;
        if (!postingHelper.hasAccounts(event.companyId(), PgcNirfChart.FROTA_COMBUSTIVEIS, PgcNirfChart.CAIXA)) {
            return;
        }
        if (postingHelper.alreadyPosted(event.companyId(), JournalSource.FLEET_FUEL, event.fuelLogId())) return;

        BigDecimal cost = safe(event.totalCost());
        if (cost.signum() <= 0) return;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(event.date() != null ? event.date() : LocalDate.now());
        entry.setDescription("Abastecimento Viatura " + event.licensePlate());
        entry.setSource(JournalSource.FLEET_FUEL);
        entry.setSourceDocumentId(event.fuelLogId());
        entry.setSourceDocumentNumber(event.licensePlate());
        entry.addLine(JournalLine.debit(postingHelper.account(event.companyId(), PgcNirfChart.FROTA_COMBUSTIVEIS),
                cost, "Consumo de combustível da frota"));
        entry.addLine(JournalLine.credit(postingHelper.cashAccount(event.companyId(), event.cashPayment()),
                cost, "Saída de tesouraria"));

        journalService.save(entry, event.companyId());
    }
}

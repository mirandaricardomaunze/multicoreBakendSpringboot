package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.model.RecurringBillingLog;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringBillingLogRepository;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Bills the next due period of one contract: locks the contract, issues the invoice, logs it and moves the next
 * billing date forward, expiring a contract past its end that does not auto-renew. Runs inside the caller's
 * transaction, which {@link RecurringBatchBillingProcessor} opens per period.
 */
@Component
public class RecurringPeriodBiller {

    record Billed(String label, BigDecimal amount) {}

    private final RecurringContractRepository contractRepository;
    private final RecurringBillingLogRepository billingLogRepository;
    private final RecurringContractInvoicer invoicer;

    public RecurringPeriodBiller(RecurringContractRepository contractRepository,
                                 RecurringBillingLogRepository billingLogRepository,
                                 RecurringContractInvoicer invoicer) {
        this.contractRepository = contractRepository;
        this.billingLogRepository = billingLogRepository;
        this.invoicer = invoicer;
    }

    /** Returns {@code null} when the contract has no period due on {@code ref} (or stopped being active). */
    Billed billNextPeriod(Long companyId, Long contractId, LocalDate ref) {
        RecurringContract contract = contractRepository.lockForBilling(companyId, contractId).orElse(null);
        if (contract == null || contract.getStatus() != ContractStatus.ACTIVE || contract.getNextBillingDate().isAfter(ref)) {
            return null;
        }
        LocalDate period = contract.getNextBillingDate();
        Invoice invoice = invoicer.invoicePeriod(contract);
        RecurringBillingLog log = newLog(contract, period, "SUCCESS",
                "Fatura " + invoice.getInvoiceNumber() + " do período de " + period + ".");
        log.setInvoice(invoice);
        log.setInvoiceNumber(invoice.getInvoiceNumber());
        log.setBilledAmount(invoice.getTotalAmount());
        billingLogRepository.save(log);

        LocalDate next = nextBillingDate(period, contract.getFrequency(), contract.getBillingDayOfMonth());
        if (contract.getEndDate() != null && next.isAfter(contract.getEndDate()) && !contract.isAutoRenew()) {
            contract.setStatus(ContractStatus.EXPIRED);
        }
        contract.setLastBilledDate(period);
        contract.setNextBillingDate(next);
        contractRepository.save(contract);
        return new Billed(invoice.getInvoiceNumber() + " (" + contract.getClient().getName() + " - "
                + invoice.getTotalAmount() + " MT)", invoice.getTotalAmount());
    }

    /** Logs a period that could not be billed and returns the message shown in the batch result. */
    String recordFailure(Long companyId, Long contractId, RuntimeException cause) {
        String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        RecurringContract contract = contractRepository.lockForBilling(companyId, contractId).orElse(null);
        if (contract == null) return "Contrato " + contractId + ": " + reason;
        billingLogRepository.save(newLog(contract, contract.getNextBillingDate(), "FAILED",
                reason.length() > 255 ? reason.substring(0, 255) : reason));
        return "Contrato " + contract.getContractNumber() + ": " + reason;
    }

    private static RecurringBillingLog newLog(RecurringContract contract, LocalDate period, String status, String notes) {
        RecurringBillingLog log = new RecurringBillingLog();
        log.setContract(contract);
        log.setBillingDate(period);
        log.setBilledAmount(BigDecimal.ZERO);
        log.setStatus(status);
        log.setNotes(notes);
        return log;
    }

    static LocalDate nextBillingDate(LocalDate currentDate, BillingFrequency frequency, int targetDay) {
        int months = frequency != null ? frequency.monthsInterval() : 1;
        YearMonth nextYm = YearMonth.from(currentDate).plusMonths(months);
        return nextYm.atDay(Math.min(targetDay, nextYm.lengthOfMonth()));
    }
}

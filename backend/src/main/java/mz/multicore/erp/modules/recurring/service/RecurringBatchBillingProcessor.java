package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the periodic billing of the active company's due contracts. Every overdue period of a contract gets its own
 * commercial invoice, issued by {@link RecurringPeriodBiller} in its own transaction: a failing period is logged
 * and reported without undoing the periods already billed or stopping the other contracts.
 */
@Component
public class RecurringBatchBillingProcessor {

    /** Upper bound on the periods one run catches up per contract, so a wrong start date cannot flood invoices. */
    static final int MAX_PERIODS_PER_RUN = 24;

    private final RecurringContractRepository contractRepository;
    private final RecurringPeriodBiller periodBiller;
    private final TransactionOperations transactions;

    public RecurringBatchBillingProcessor(RecurringContractRepository contractRepository,
                                          RecurringPeriodBiller periodBiller, TransactionOperations transactions) {
        this.contractRepository = contractRepository;
        this.periodBiller = periodBiller;
        this.transactions = transactions;
    }

    public BatchBillingResultDTO processBatchBilling(Long companyId, LocalDate referenceDate) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        List<Long> due = contractRepository.findDueContractIds(companyId, ContractStatus.ACTIVE, ref);

        List<String> invoices = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Long contractId : due) {
            for (int period = 0; period < MAX_PERIODS_PER_RUN; period++) {
                RecurringPeriodBiller.Billed billed;
                try {
                    billed = transactions.execute(status -> periodBiller.billNextPeriod(companyId, contractId, ref));
                } catch (RuntimeException e) {
                    errors.add(transactions.execute(status -> periodBiller.recordFailure(companyId, contractId, e)));
                    break;
                }
                if (billed == null) break;
                invoices.add(billed.label());
                total = total.add(billed.amount());
            }
        }
        return new BatchBillingResultDTO(ref, due.size(), invoices.size(), total, invoices, errors);
    }
}

package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
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
import java.util.ArrayList;
import java.util.List;

@Component
public class RecurringBatchBillingProcessor {

    private final RecurringContractRepository contractRepository;
    private final RecurringBillingLogRepository billingLogRepository;

    public RecurringBatchBillingProcessor(
            RecurringContractRepository contractRepository,
            RecurringBillingLogRepository billingLogRepository
    ) {
        this.contractRepository = contractRepository;
        this.billingLogRepository = billingLogRepository;
    }

    public BatchBillingResultDTO processBatchBilling(Long companyId, LocalDate referenceDate) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        LocalDate effectiveRef = (referenceDate != null) ? referenceDate : LocalDate.now();

        List<RecurringContract> dueContracts = contractRepository.findDueContracts(
                companyId, ContractStatus.ACTIVE, effectiveRef);

        List<String> generatedInvoices = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        BigDecimal totalBilled = BigDecimal.ZERO;
        int processedCount = 0;

        for (RecurringContract contract : dueContracts) {
            try {
                processedCount++;
                String invoiceNumber = "FT-" + contract.getContractNumber() + "-" + effectiveRef.toString().replace("-", "");

                RecurringBillingLog log = new RecurringBillingLog();
                log.setContract(contract);
                log.setBillingDate(effectiveRef);
                log.setBilledAmount(contract.getFixedAmount());
                log.setInvoiceNumber(invoiceNumber);
                log.setStatus("SUCCESS");
                log.setNotes("Facturação periódica emitida para o contrato " + contract.getContractNumber());
                billingLogRepository.save(log);

                contract.setLastBilledDate(effectiveRef);
                LocalDate nextDate = computeNextBillingDate(
                        contract.getNextBillingDate(), contract.getFrequency(), contract.getBillingDayOfMonth());

                if (contract.getEndDate() != null && nextDate.isAfter(contract.getEndDate()) && !contract.isAutoRenew()) {
                    contract.setStatus(ContractStatus.EXPIRED);
                }
                contract.setNextBillingDate(nextDate);
                contractRepository.save(contract);

                totalBilled = totalBilled.add(contract.getFixedAmount());
                generatedInvoices.add(invoiceNumber + " (" + contract.getClient().getName() + " - " + contract.getFixedAmount() + " MT)");
            } catch (Exception e) {
                errors.add("Erro no contrato " + contract.getContractNumber() + ": " + e.getMessage());
            }
        }

        return new BatchBillingResultDTO(effectiveRef, processedCount, generatedInvoices.size(), totalBilled, generatedInvoices, errors);
    }

    private LocalDate computeNextBillingDate(LocalDate currentDate, BillingFrequency frequency, int targetDay) {
        int months = frequency != null ? frequency.monthsInterval() : 1;
        YearMonth nextYm = YearMonth.from(currentDate).plusMonths(months);
        return nextYm.atDay(Math.min(targetDay, nextYm.lengthOfMonth()));
    }
}

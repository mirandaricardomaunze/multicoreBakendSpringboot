package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Single entry point for recurring contracts: lifecycle in {@link RecurringContractLifecycleService}, periodic
 * billing in {@link RecurringBatchBillingProcessor}.
 */
@Service
public class RecurringContractService {

    private final RecurringContractLifecycleService lifecycleService;
    private final RecurringBatchBillingProcessor batchBillingProcessor;

    public RecurringContractService(
            RecurringContractLifecycleService lifecycleService,
            RecurringBatchBillingProcessor batchBillingProcessor
    ) {
        this.lifecycleService = lifecycleService;
        this.batchBillingProcessor = batchBillingProcessor;
    }

    @Transactional(readOnly = true)
    public List<RecurringContractDTO> listContracts(Long companyId) {
        return lifecycleService.listContracts(companyId);
    }

    @Transactional(readOnly = true)
    public RecurringContractDTO getContract(Long companyId, Long id) {
        return lifecycleService.getContract(companyId, id);
    }

    @Transactional
    public RecurringContractDTO createContract(Long companyId, CreateRecurringContractRequest request) {
        return lifecycleService.createContract(companyId, request);
    }

    @Transactional
    public RecurringContractDTO updateStatus(Long companyId, Long id, ContractStatus newStatus) {
        return lifecycleService.updateStatus(companyId, id, newStatus);
    }

    /** Not transactional on purpose: the processor gives every billed period its own transaction. */
    public BatchBillingResultDTO processBatchBilling(Long companyId, LocalDate referenceDate) {
        return batchBillingProcessor.processBatchBilling(companyId, referenceDate);
    }
}

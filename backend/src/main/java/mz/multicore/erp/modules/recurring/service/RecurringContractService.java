package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.repository.RecurringBillingLogRepository;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringContractService {

    private final RecurringContractLifecycleService lifecycleService;
    private final RecurringBatchBillingProcessor batchBillingProcessor;

    @Autowired
    public RecurringContractService(
            RecurringContractLifecycleService lifecycleService,
            RecurringBatchBillingProcessor batchBillingProcessor
    ) {
        this.lifecycleService = lifecycleService;
        this.batchBillingProcessor = batchBillingProcessor;
    }

    public RecurringContractService(
            RecurringContractRepository contractRepository,
            RecurringBillingLogRepository billingLogRepository,
            CompanyRepository companyRepository,
            ClientRepository clientRepository
    ) {
        this(
                new RecurringContractLifecycleService(contractRepository, companyRepository, clientRepository),
                new RecurringBatchBillingProcessor(contractRepository, billingLogRepository)
        );
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

    @Transactional
    public BatchBillingResultDTO processBatchBilling(Long companyId, LocalDate referenceDate) {
        return batchBillingProcessor.processBatchBilling(companyId, referenceDate);
    }
}

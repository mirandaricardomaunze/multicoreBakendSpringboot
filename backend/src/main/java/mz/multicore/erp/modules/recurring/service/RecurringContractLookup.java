package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura dos contratos de facturação recorrente para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
@Transactional(readOnly = true)
public class RecurringContractLookup {

    private final RecurringContractRepository contractRepository;

    public RecurringContractLookup(RecurringContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    /** Contratos da empresa com o cliente já carregado. */
    public List<RecurringContract> listForCompanyWithClient(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return contractRepository.findByCompanyIdWithClient(companyId);
    }
}

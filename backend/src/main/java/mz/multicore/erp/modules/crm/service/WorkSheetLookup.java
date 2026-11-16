package mz.multicore.erp.modules.crm.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.crm.model.WorkSheet;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Porta de leitura das folhas de obra para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
@Transactional(readOnly = true)
public class WorkSheetLookup {

    private static final String NOT_FOUND = "Folha de obra não encontrada.";

    private final WorkSheetRepository workSheetRepository;

    public WorkSheetLookup(WorkSheetRepository workSheetRepository) {
        this.workSheetRepository = workSheetRepository;
    }

    /** Folha de obra cujo pedido de suporte pertence à empresa. */
    public WorkSheet requireForCompany(Long workSheetId, Long companyId) {
        if (workSheetId == null || companyId == null) {
            throw new BusinessRuleException(NOT_FOUND);
        }
        return workSheetRepository.findByIdAndSupportTicketCompanyId(workSheetId, companyId)
                .orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }
}

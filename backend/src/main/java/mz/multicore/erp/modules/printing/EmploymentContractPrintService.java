package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF do contrato de trabalho. Ver docs/RH_COMPLETO_SPEC.md §B1.
 */
@Service
public class EmploymentContractPrintService {

    private final EmploymentContractRepository contractRepository;
    private final EmploymentContractDocumentRenderer documentRenderer;

    @Autowired
    public EmploymentContractPrintService(EmploymentContractRepository contractRepository,
                                          EmploymentContractDocumentRenderer documentRenderer) {
        this.contractRepository = contractRepository;
        this.documentRenderer = documentRenderer;
    }

    public EmploymentContractPrintService(EmploymentContractRepository contractRepository) {
        this(contractRepository, new EmploymentContractDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long contractId) {
        EmploymentContract contract = contractRepository
                .findByIdWithEmployeeAndCompanyId(contractId, CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado."));
        return documentRenderer.renderA4(contract);
    }
}

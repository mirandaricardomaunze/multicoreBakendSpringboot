package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.service.EmployeeLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF do contrato de trabalho. Ver docs/RH_COMPLETO_SPEC.md §B1.
 */
@Service
public class EmploymentContractPrintService {

    private final EmployeeLookup employeeLookup;
    private final EmploymentContractDocumentRenderer documentRenderer;

    @Autowired
    public EmploymentContractPrintService(EmployeeLookup employeeLookup,
                                          EmploymentContractDocumentRenderer documentRenderer) {
        this.employeeLookup = employeeLookup;
        this.documentRenderer = documentRenderer;
    }

    public EmploymentContractPrintService(EmployeeLookup employeeLookup) {
        this(employeeLookup, new EmploymentContractDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long contractId) {
        EmploymentContract contract = employeeLookup
                .requireContractForCompany(contractId, CurrentUserContext.requireCurrentCompanyId());
        return documentRenderer.renderA4(contract);
    }
}

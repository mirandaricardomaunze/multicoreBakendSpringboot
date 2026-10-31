package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.service.BankReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de geração de Folha Oficial de Reconciliação Bancária em PDF A4.
 */
@Service
public class BankReconciliationPrintService {

    private final BankReconciliationService reconciliationService;
    private final CompanyService companyService;
    private final BankReconciliationDocumentRenderer documentRenderer;

    @Autowired
    public BankReconciliationPrintService(
            BankReconciliationService reconciliationService,
            CompanyService companyService,
            BankReconciliationDocumentRenderer documentRenderer
    ) {
        this.reconciliationService = reconciliationService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public BankReconciliationPrintService(
            BankReconciliationService reconciliationService,
            CompanyService companyService
    ) {
        this(reconciliationService, companyService, new BankReconciliationDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long statementId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }
        BankStatementDTO statement = reconciliationService.getStatement(statementId);
        List<BankStatementItemDTO> items = reconciliationService.getStatementItems(statementId);
        return documentRenderer.renderDocument(company, statement, items);
    }
}

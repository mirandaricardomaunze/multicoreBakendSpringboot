package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.service.SupplierStatementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Serviço de geração de Extrato Canónico de Conta Corrente e Reconciliação de Fornecedores em PDF A4.
 */
@Service
public class SupplierStatementPrintService {

    private final SupplierStatementService statementService;
    private final CompanyService companyService;
    private final SupplierStatementDocumentRenderer documentRenderer;

    @Autowired
    public SupplierStatementPrintService(
            SupplierStatementService statementService,
            CompanyService companyService,
            SupplierStatementDocumentRenderer documentRenderer
    ) {
        this.statementService = statementService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public SupplierStatementPrintService(
            SupplierStatementService statementService,
            CompanyService companyService
    ) {
        this(statementService, companyService, new SupplierStatementDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long supplierId, LocalDate startDate, LocalDate endDate) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }
        SupplierStatementDTO statement = statementService.getStatement(supplierId, startDate, endDate);
        return documentRenderer.renderDocument(company, statement);
    }
}

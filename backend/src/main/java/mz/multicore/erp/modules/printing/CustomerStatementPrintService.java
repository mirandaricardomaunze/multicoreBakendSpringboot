package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.service.CustomerStatementService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Fachada para emissão PDF A4 do Extrato Canónico de Conta Corrente de Clientes. */
@Service
public class CustomerStatementPrintService {

    private final CustomerStatementService statementService;
    private final CompanyService companyService;
    private final CustomerStatementDocumentRenderer documentRenderer;

    @Autowired
    public CustomerStatementPrintService(CustomerStatementService statementService,
                                         CompanyService companyService,
                                         CustomerStatementDocumentRenderer documentRenderer) {
        this.statementService = statementService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public CustomerStatementPrintService(CustomerStatementService statementService,
                                         CompanyService companyService) {
        this(statementService, companyService, new CustomerStatementDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long clientId, LocalDate startDate, LocalDate endDate) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }
        CustomerStatementDTO statement = statementService.getStatement(clientId, startDate, endDate);
        return documentRenderer.render(company, statement);
    }
}

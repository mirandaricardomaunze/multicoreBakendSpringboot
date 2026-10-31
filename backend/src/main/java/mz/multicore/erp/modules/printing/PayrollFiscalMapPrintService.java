package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.hr.dto.PayrollFiscalSummaryDTO;
import mz.multicore.erp.modules.hr.service.PayrollTaxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mapa fiscal salarial mensal (INSS + IRPS por colaborador) para entrega às autoridades. */
@Service
public class PayrollFiscalMapPrintService {

    private final PayrollTaxService payrollTaxService;
    private final CompanyService companyService;
    private final PayrollFiscalMapDocumentRenderer documentRenderer;

    @Autowired
    public PayrollFiscalMapPrintService(PayrollTaxService payrollTaxService,
                                        CompanyService companyService,
                                        PayrollFiscalMapDocumentRenderer documentRenderer) {
        this.payrollTaxService = payrollTaxService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public PayrollFiscalMapPrintService(PayrollTaxService payrollTaxService, CompanyService companyService) {
        this(payrollTaxService, companyService, new PayrollFiscalMapDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int year, int month) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        PayrollFiscalSummaryDTO summary = payrollTaxService.fiscalSummary(year, month);
        return documentRenderer.render(company, summary, year, month);
    }
}

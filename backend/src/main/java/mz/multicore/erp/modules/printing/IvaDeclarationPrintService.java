package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.fiscal.dto.IvaSummaryDTO;
import mz.multicore.erp.modules.fiscal.service.FiscalSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Declaração mensal de IVA (Modelo simplificado, para entrega à AT). */
@Service
public class IvaDeclarationPrintService {

    private final FiscalSummaryService fiscalSummaryService;
    private final CompanyService companyService;
    private final IvaDeclarationDocumentRenderer documentRenderer;

    @Autowired
    public IvaDeclarationPrintService(
            FiscalSummaryService fiscalSummaryService,
            CompanyService companyService,
            IvaDeclarationDocumentRenderer documentRenderer
    ) {
        this.fiscalSummaryService = fiscalSummaryService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public IvaDeclarationPrintService(FiscalSummaryService fiscalSummaryService, CompanyService companyService) {
        this(fiscalSummaryService, companyService, new IvaDeclarationDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int year, int month) {
        return render(companyId, year, month, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int year, int month, BigDecimal previousCredit) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        IvaSummaryDTO summary = fiscalSummaryService.computeMonth(companyId, year, month, previousCredit);
        return documentRenderer.renderDocument(company, summary, year, month);
    }
}

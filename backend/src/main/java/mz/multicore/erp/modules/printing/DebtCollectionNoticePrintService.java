package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import mz.multicore.erp.modules.comercial.service.CreditRiskService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Serviço de geração de Notificação Formal de Cobrança e Regularização de Dívida em PDF A4.
 */
@Service
public class DebtCollectionNoticePrintService {

    private final CreditRiskService creditRiskService;
    private final CompanyService companyService;
    private final DebtCollectionNoticeDocumentRenderer documentRenderer;

    @Autowired
    public DebtCollectionNoticePrintService(
            CreditRiskService creditRiskService,
            CompanyService companyService,
            DebtCollectionNoticeDocumentRenderer documentRenderer
    ) {
        this.creditRiskService = creditRiskService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public DebtCollectionNoticePrintService(
            CreditRiskService creditRiskService,
            CompanyService companyService
    ) {
        this(creditRiskService, companyService, new DebtCollectionNoticeDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long clientId, LocalDate referenceDate) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }
        DebtCollectionNoticeDTO notice = creditRiskService.getDebtCollectionNotice(clientId, referenceDate);
        return documentRenderer.renderDocument(company, notice);
    }
}

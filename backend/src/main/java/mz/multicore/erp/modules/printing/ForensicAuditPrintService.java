package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import mz.multicore.erp.modules.audit.service.ForensicAuditService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ForensicAuditPrintService {

    private final ForensicAuditService auditService;
    private final CompanyService companyService;
    private final ForensicAuditDocumentRenderer documentRenderer;

    @Autowired
    public ForensicAuditPrintService(
            ForensicAuditService auditService,
            CompanyService companyService,
            ForensicAuditDocumentRenderer documentRenderer
    ) {
        this.auditService = auditService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public ForensicAuditPrintService(
            ForensicAuditService auditService,
            CompanyService companyService
    ) {
        this(auditService, companyService, new ForensicAuditDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate,
            ForensicSeverity severityFilter,
            ForensicCategory categoryFilter,
            String operatorFilter
    ) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }
        ForensicAuditSummaryDTO summary = auditService.getForensicSummaryForCompany(
                companyId, startDate, endDate, severityFilter, categoryFilter, operatorFilter);
        return documentRenderer.renderDocument(company, summary, startDate, endDate);
    }
}

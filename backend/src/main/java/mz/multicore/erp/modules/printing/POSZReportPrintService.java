package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.service.POSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada tenant-scoped para emissão do documento de <b>Fecho de Caixa (Z)</b>.
 * Ver {@code docs/FECHO_CAIXA_Z_SPEC.md}.
 */
@Service
public class POSZReportPrintService {

    private final POSService posService;
    private final CompanyService companyService;
    private final PosZReportDocumentRenderer documentRenderer;

    @Autowired
    public POSZReportPrintService(POSService posService, CompanyService companyService,
                                  PosZReportDocumentRenderer documentRenderer) {
        this.posService = posService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public POSZReportPrintService(POSService posService, CompanyService companyService) {
        this(posService, companyService, new PosZReportDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long sessionId) {
        PosZReportDTO z = posService.buildZReport(sessionId);
        Company company = companyService.getCompanyById(CurrentUserContext.getCurrentCompanyId());
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        return documentRenderer.render(company, z);
    }
}

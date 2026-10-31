package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.InventorySessionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryPhysicalCountingPrintService {

    private final CompanyService companyService;
    private final InventoryPhysicalCountingDocumentRenderer documentRenderer;

    @Autowired
    public InventoryPhysicalCountingPrintService(CompanyService companyService,
                                                 InventoryPhysicalCountingDocumentRenderer documentRenderer) {
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public InventoryPhysicalCountingPrintService(CompanyService companyService) {
        this(companyService, new InventoryPhysicalCountingDocumentRenderer());
    }

    public byte[] render(InventorySessionDTO session, Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        return documentRenderer.render(company, session);
    }
}

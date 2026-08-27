package mz.multicore.erp.modules.company.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.TenantAccessService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final TenantAccessService tenantAccessService;

    public CompanyService(CompanyRepository companyRepository, TenantAccessService tenantAccessService) {
        this.companyRepository = companyRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return tenantAccessService.getAccessibleCompanies(CurrentUserContext.getUsername());
    }

    @Transactional(readOnly = true)
    public Company getCompanyById(Long id) {
        tenantAccessService.requireAccess(CurrentUserContext.getUsername(), id);
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }

    /**
     * Resolve a empresa do tenant activo para associações entre agregados.
     * Mantém o acesso ao Repository dentro do domínio company.
     */
    @Transactional(readOnly = true)
    public Company getCurrentCompanyReference(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }

    public void selectCompany(Long id) {
        tenantAccessService.selectCompany(id);
    }
}

package com.phcpro.modules.company.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.TenantAccessService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
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

    public void selectCompany(Long id) {
        tenantAccessService.selectCompany(id);
    }
}

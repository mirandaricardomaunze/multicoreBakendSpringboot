package com.phcpro.modules.platform.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.platform.dto.CreateCompanyRequest;
import com.phcpro.modules.platform.dto.PlatformCompanyDTO;
import com.phcpro.modules.platform.dto.UpdateCompanyRequest;
import com.phcpro.modules.users.repository.AppUserCompanyAccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Gestão de empresas ao nível da plataforma (superadmin): listar todas, activar/desactivar e
 * onboarding/edição. Todas as operações exigem o papel SUPERADMIN e são auditadas.
 */
@Service
public class PlatformCompanyService {

    private final CompanyRepository companyRepository;
    private final AppUserCompanyAccessRepository companyAccessRepository;
    private final AuditLogService auditLogService;

    public PlatformCompanyService(CompanyRepository companyRepository,
                                  AppUserCompanyAccessRepository companyAccessRepository,
                                  AuditLogService auditLogService) {
        this.companyRepository = companyRepository;
        this.companyAccessRepository = companyAccessRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PlatformCompanyDTO> listCompanies() {
        PermissionGuard.requireSuperAdmin("listar empresas");
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PlatformCompanyDTO setCompanyActive(Long companyId, boolean active) {
        PermissionGuard.requireSuperAdmin("mudar o estado de uma empresa");
        Company company = requireCompany(companyId);
        company.setActive(active);
        companyRepository.save(company);
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "PLATFORM_COMPANY_STATUS",
                String.format("Empresa '%s' %s.", company.getName(), active ? "activada" : "desactivada"));
        return toDto(company);
    }

    @Transactional
    public PlatformCompanyDTO createCompany(CreateCompanyRequest request) {
        PermissionGuard.requireSuperAdmin("criar uma empresa");
        String taxId = requireText(request.taxId(), "O NUIT é obrigatório.");
        companyRepository.findByTaxId(taxId).ifPresent(existing -> {
            throw new BusinessRuleException("Já existe uma empresa com o NUIT " + taxId + ".");
        });
        Company company = new Company();
        company.setName(requireText(request.name(), "O nome da empresa é obrigatório."));
        company.setTaxId(taxId);
        company.setEmail(request.email());
        company.setAddress(request.address());
        company.setActive(true);
        company.setCreatedBy(CurrentUserContext.getUsername());
        company = companyRepository.save(company);
        auditLogService.logEvent(CurrentUserContext.getUsername(), company.getId(), "PLATFORM_COMPANY_CREATE",
                "Empresa criada: " + company.getName());
        return toDto(company);
    }

    @Transactional
    public PlatformCompanyDTO updateCompany(Long companyId, UpdateCompanyRequest request) {
        PermissionGuard.requireSuperAdmin("editar uma empresa");
        Company company = requireCompany(companyId);
        company.setName(requireText(request.name(), "O nome da empresa é obrigatório."));
        company.setEmail(request.email());
        company.setAddress(request.address());
        companyRepository.save(company);
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "PLATFORM_COMPANY_UPDATE",
                "Empresa actualizada: " + company.getName());
        return toDto(company);
    }

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }
        return value.trim();
    }

    private PlatformCompanyDTO toDto(Company company) {
        return new PlatformCompanyDTO(
                company.getId(), company.getName(), company.getTaxId(), company.getEmail(),
                company.getAddress(), company.isActive(),
                companyAccessRepository.countByCompanyId(company.getId()));
    }
}

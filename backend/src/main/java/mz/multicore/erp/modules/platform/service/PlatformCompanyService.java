package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.platform.dto.UpdateCompanyRequest;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
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
        company.setPhone(request.phone());
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
        company.setPhone(request.phone());
        companyRepository.save(company);
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "PLATFORM_COMPANY_UPDATE",
                "Empresa actualizada: " + company.getName());
        return toDto(company);
    }

    /** Carrega/actualiza o logótipo da empresa (imagem já reduzida no cliente). */
    @Transactional
    public void updateCompanyLogo(Long companyId, byte[] logo) {
        PermissionGuard.requireSuperAdmin("actualizar o logótipo da empresa");
        Company company = requireCompany(companyId);
        company.setLogo(logo != null && logo.length > 0 ? logo : null);
        companyRepository.save(company);
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "PLATFORM_COMPANY_LOGO",
                "Logótipo actualizado: " + company.getName());
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
                company.getAddress(), company.getPhone(),
                company.getLogo() != null && company.getLogo().length > 0,
                company.isActive(),
                companyAccessRepository.countByCompanyId(company.getId()));
    }
}

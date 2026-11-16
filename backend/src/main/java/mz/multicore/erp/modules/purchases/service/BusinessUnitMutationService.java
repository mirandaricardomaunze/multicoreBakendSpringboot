package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.dto.CreateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.dto.UpdateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Component;

@Component
public class BusinessUnitMutationService {

    private final BusinessUnitRepository businessUnitRepository;
    private final SupplierRepository supplierRepository;
    private final CompanyLookup companyLookup;
    private final AuditLogService auditLogService;
    private final BusinessUnitQueryAndPolicyService queryAndPolicyService;

    public BusinessUnitMutationService(BusinessUnitRepository businessUnitRepository,
                                       SupplierRepository supplierRepository,
                                       CompanyLookup companyLookup,
                                       AuditLogService auditLogService,
                                       BusinessUnitQueryAndPolicyService queryAndPolicyService) {
        this.businessUnitRepository = businessUnitRepository;
        this.supplierRepository = supplierRepository;
        this.companyLookup = companyLookup;
        this.auditLogService = auditLogService;
        this.queryAndPolicyService = queryAndPolicyService;
    }

    public BusinessUnitDTO create(CreateBusinessUnitRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        String code = request.code().trim().toUpperCase();
        if (businessUnitRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new BusinessRuleException("Já existe uma linha de negócio com o código '" + code + "' nesta empresa.");
        }

        Company company = companyLookup.require(companyId);

        Supplier supplier = resolveSupplier(request.supplierId());

        BusinessUnit unit = new BusinessUnit();
        unit.setCompany(company);
        unit.setCode(code);
        unit.setName(request.name().trim());
        unit.setDescription(request.description() != null ? request.description().trim() : null);
        unit.setSupplier(supplier);
        unit.setActive(true);

        BusinessUnit saved = businessUnitRepository.save(unit);
        auditLogService.logCurrent("BUSINESS_UNIT_CREATED", "Linha de negócio criada: " + saved.getCode() + " - " + saved.getName());
        return queryAndPolicyService.toDTO(saved);
    }

    public BusinessUnitDTO update(Long id, UpdateBusinessUnitRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        BusinessUnit unit = businessUnitRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Unidade de negócio não encontrada."));

        String newCode = request.code().trim().toUpperCase();
        if (!unit.getCode().equalsIgnoreCase(newCode) && businessUnitRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, newCode)) {
            throw new BusinessRuleException("Já existe outra linha de negócio com o código '" + newCode + "'.");
        }

        Supplier supplier = resolveSupplier(request.supplierId());

        unit.setCode(newCode);
        unit.setName(request.name().trim());
        unit.setDescription(request.description() != null ? request.description().trim() : null);
        unit.setSupplier(supplier);
        if (request.active() != null) {
            unit.setActive(request.active());
        }

        BusinessUnit saved = businessUnitRepository.save(unit);
        auditLogService.logCurrent("BUSINESS_UNIT_UPDATED", "Linha de negócio actualizada: " + saved.getCode());
        return queryAndPolicyService.toDTO(saved);
    }

    public void toggleActive(Long id) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        BusinessUnit unit = businessUnitRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Unidade de negócio não encontrada."));
        unit.setActive(!unit.isActive());
        businessUnitRepository.save(unit);
        auditLogService.logCurrent("BUSINESS_UNIT_TOGGLE", "Linha de negócio " + unit.getCode() + " " + (unit.isActive() ? "activada" : "desactivada"));
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessRuleException("Fornecedor associado não encontrado."));
        CurrentUserContext.requireCompany(supplier.getCompany().getId());
        return supplier;
    }
}

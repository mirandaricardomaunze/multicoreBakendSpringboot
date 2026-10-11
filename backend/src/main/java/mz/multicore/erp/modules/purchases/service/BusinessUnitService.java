package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.dto.CreateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.dto.UpdateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessUnitService {

    private final BusinessUnitRepository businessUnitRepository;
    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public BusinessUnitService(BusinessUnitRepository businessUnitRepository,
                               SupplierRepository supplierRepository,
                               CompanyRepository companyRepository,
                               ProductRepository productRepository,
                               AuditLogService auditLogService) {
        this.businessUnitRepository = businessUnitRepository;
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<BusinessUnitDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        List<BusinessUnit> units = businessUnitRepository.findByCompanyId(companyId);
        return units.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessUnitDTO> findActiveByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        List<BusinessUnit> units = businessUnitRepository.findByCompanyIdAndActiveTrue(companyId);
        return units.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public BusinessUnitDTO findById(Long id) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        BusinessUnit unit = businessUnitRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Unidade de negócio não encontrada."));
        return toDTO(unit);
    }

    @Transactional
    public BusinessUnitDTO create(CreateBusinessUnitRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        String code = request.code().trim().toUpperCase();
        if (businessUnitRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new BusinessRuleException("Já existe uma linha de negócio com o código '" + code + "' nesta empresa.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new BusinessRuleException("Fornecedor associado não encontrado."));
            CurrentUserContext.requireCompany(supplier.getCompany().getId());
        }

        BusinessUnit unit = new BusinessUnit();
        unit.setCompany(company);
        unit.setCode(code);
        unit.setName(request.name().trim());
        unit.setDescription(request.description() != null ? request.description().trim() : null);
        unit.setSupplier(supplier);
        unit.setActive(true);

        BusinessUnit saved = businessUnitRepository.save(unit);
        auditLogService.logCurrent("BUSINESS_UNIT_CREATED", "Linha de negócio criada: " + saved.getCode() + " - " + saved.getName());
        return toDTO(saved);
    }

    @Transactional
    public BusinessUnitDTO update(Long id, UpdateBusinessUnitRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        BusinessUnit unit = businessUnitRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Unidade de negócio não encontrada."));

        String newCode = request.code().trim().toUpperCase();
        if (!unit.getCode().equalsIgnoreCase(newCode) && businessUnitRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, newCode)) {
            throw new BusinessRuleException("Já existe outra linha de negócio com o código '" + newCode + "'.");
        }

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new BusinessRuleException("Fornecedor associado não encontrado."));
            CurrentUserContext.requireCompany(supplier.getCompany().getId());
        }

        unit.setCode(newCode);
        unit.setName(request.name().trim());
        unit.setDescription(request.description() != null ? request.description().trim() : null);
        unit.setSupplier(supplier);
        if (request.active() != null) {
            unit.setActive(request.active());
        }

        BusinessUnit saved = businessUnitRepository.save(unit);
        auditLogService.logCurrent("BUSINESS_UNIT_UPDATED", "Linha de negócio actualizada: " + saved.getCode());
        return toDTO(saved);
    }

    @Transactional
    public void toggleActive(Long id) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        BusinessUnit unit = businessUnitRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Unidade de negócio não encontrada."));
        unit.setActive(!unit.isActive());
        businessUnitRepository.save(unit);
        auditLogService.logCurrent("BUSINESS_UNIT_TOGGLE", "Linha de negócio " + unit.getCode() + " " + (unit.isActive() ? "activada" : "desactivada"));
    }

    /**
     * Valida se um produto pode ser inserido num documento restrito a uma linha de negócio.
     * Se a restrição estiver ativa (businessUnitId != null), o produto DEVE pertencer a essa unidade.
     */
    public void validateProductRestriction(Long restrictedBusinessUnitId, Product product) {
        if (restrictedBusinessUnitId == null) {
            return; // Modo misto: aceita produtos de quaisquer negócios
        }
        BusinessUnit productBu = product.getBusinessUnit();
        if (productBu == null || !restrictedBusinessUnitId.equals(productBu.getId())) {
            BusinessUnit restrictedBu = businessUnitRepository.findById(restrictedBusinessUnitId).orElse(null);
            String buName = restrictedBu != null ? restrictedBu.getName() : "#" + restrictedBusinessUnitId;
            String prodBuName = productBu != null ? productBu.getName() : "Nenhum";
            throw new BusinessRuleException("A faturação está restrita à linha de negócio '" + buName
                    + "' e não aceita o produto '" + product.getName()
                    + "' (pertencente a: " + prodBuName + "). Desmarque o filtro de negócio para emitir produtos mistos.");
        }
    }

    private BusinessUnitDTO toDTO(BusinessUnit unit) {
        long count = productRepository.countByBusinessUnitId(unit.getId());
        return new BusinessUnitDTO(
                unit.getId(),
                unit.getCode(),
                unit.getName(),
                unit.getSupplier() != null ? unit.getSupplier().getId() : null,
                unit.getSupplier() != null ? unit.getSupplier().getName() : null,
                unit.getDescription(),
                unit.isActive(),
                count
        );
    }
}

package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.architecture.validation.TaxIdValidator;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierManagementService {
    private final SupplierRepository supplierRepository;
    private final CompanyLookup companyLookup;
    private final AuditLogService auditLogService;

    public SupplierManagementService(SupplierRepository supplierRepository,
                                   CompanyLookup companyLookup,
                                   AuditLogService auditLogService) {
        this.supplierRepository = supplierRepository;
        this.companyLookup = companyLookup;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Supplier createSupplier(String name, String taxId, String email, String address, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        TaxIdValidator.validate(taxId);
        Company company = companyLookup.require(companyId);

        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setTaxId(taxId);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setCompany(company);
        supplier.setCreatedBy("SYSTEM");

        return supplierRepository.save(supplier);
    }

    @Transactional
    public SupplierDTO createSupplier(CreateSupplierRequest request) {
        Supplier supplier = createSupplier(request.name(), request.taxId(), request.email(), request.address(), request.companyId());
        supplier.setPhone(request.phone());
        supplier.setContactPerson(request.contactPerson());
        supplier = supplierRepository.save(supplier);
        auditLogService.logCurrent("SUPPLIER_CREATE", "Fornecedor " + supplier.getName() + " (" + supplier.getTaxId() + ")");
        return toDTO(supplier);
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, CreateSupplierRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        TaxIdValidator.validate(request.taxId());
        Supplier supplier = loadSupplierForCompany(id, request.companyId());
        supplier.setName(request.name());
        supplier.setTaxId(request.taxId());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setPhone(request.phone());
        supplier.setContactPerson(request.contactPerson());
        supplier = supplierRepository.save(supplier);
        auditLogService.logCurrent("SUPPLIER_UPDATE", "Fornecedor #" + id + " " + supplier.getName());
        return toDTO(supplier);
    }

    @Transactional
    public SupplierDTO setSupplierActive(Long id, Long companyId, boolean active) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireManagerOrAdmin(active ? "activar fornecedor" : "desactivar fornecedor");
        Supplier supplier = loadSupplierForCompany(id, companyId);
        supplier.setActive(active);
        supplier = supplierRepository.save(supplier);
        auditLogService.logCurrent("SUPPLIER_STATUS",
                "Fornecedor #" + id + " -> " + (active ? "ACTIVO" : "INACTIVO"));
        return toDTO(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> searchSuppliers(Long companyId, String query) {
        CurrentUserContext.requireCompany(companyId);
        String term = query == null ? "" : query.trim().toLowerCase();
        return supplierRepository.findByCompanyId(companyId).stream()
                .filter(s -> term.isEmpty()
                        || (s.getName() != null && s.getName().toLowerCase().contains(term))
                        || (s.getTaxId() != null && s.getTaxId().toLowerCase().contains(term)))
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Supplier> getSuppliersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return supplierRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> findSuppliersByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return supplierRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    public Supplier loadSupplierForCompany(Long id, Long companyId) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado."));
        if (supplier.getCompany() == null || !companyId.equals(supplier.getCompany().getId())) {
            throw new BusinessRuleException("O fornecedor não pertence à empresa ativa.");
        }
        return supplier;
    }

    public SupplierDTO toDTO(Supplier s) {
        return new SupplierDTO(
                s.getId(),
                s.getName(),
                s.getTaxId(),
                s.getEmail(),
                s.getAddress(),
                s.getPhone(),
                s.getContactPerson(),
                s.isActive(),
                s.getCompany() != null ? s.getCompany().getId() : null,
                s.getVersion()
        );
    }
}

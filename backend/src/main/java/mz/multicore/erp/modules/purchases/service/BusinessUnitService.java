package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.dto.CreateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.dto.UpdateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessUnitService {

    private final BusinessUnitMutationService mutationService;
    private final BusinessUnitQueryAndPolicyService queryAndPolicyService;

    @Autowired
    public BusinessUnitService(BusinessUnitMutationService mutationService,
                               BusinessUnitQueryAndPolicyService queryAndPolicyService) {
        this.mutationService = mutationService;
        this.queryAndPolicyService = queryAndPolicyService;
    }

    public BusinessUnitService(BusinessUnitRepository businessUnitRepository,
                               SupplierRepository supplierRepository,
                               CompanyLookup companyLookup,
                               ProductLookup productLookup,
                               AuditLogService auditLogService) {
        BusinessUnitQueryAndPolicyService qp = new BusinessUnitQueryAndPolicyService(businessUnitRepository, productLookup);
        this.queryAndPolicyService = qp;
        this.mutationService = new BusinessUnitMutationService(
                businessUnitRepository, supplierRepository, companyLookup, auditLogService, qp);
    }

    @Transactional(readOnly = true)
    public List<BusinessUnitDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryAndPolicyService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<BusinessUnitDTO> findActiveByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryAndPolicyService.findActiveByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public BusinessUnitDTO findById(Long id) {
        CurrentUserContext.requireCurrentCompanyId();
        return queryAndPolicyService.findById(id);
    }

    @Transactional
    public BusinessUnitDTO create(CreateBusinessUnitRequest request) {
        CurrentUserContext.requireCurrentCompanyId();
        return mutationService.create(request);
    }

    @Transactional
    public BusinessUnitDTO update(Long id, UpdateBusinessUnitRequest request) {
        CurrentUserContext.requireCurrentCompanyId();
        return mutationService.update(id, request);
    }

    @Transactional
    public void toggleActive(Long id) {
        CurrentUserContext.requireCurrentCompanyId();
        mutationService.toggleActive(id);
    }

    public void validateProductRestriction(Long restrictedBusinessUnitId, Product product) {
        queryAndPolicyService.validateProductRestriction(restrictedBusinessUnitId, product);
    }
}

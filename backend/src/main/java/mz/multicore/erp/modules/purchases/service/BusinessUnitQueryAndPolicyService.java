package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BusinessUnitQueryAndPolicyService {

    private final BusinessUnitRepository businessUnitRepository;
    private final ProductRepository productRepository;

    public BusinessUnitQueryAndPolicyService(BusinessUnitRepository businessUnitRepository,
                                             ProductRepository productRepository) {
        this.businessUnitRepository = businessUnitRepository;
        this.productRepository = productRepository;
    }

    public List<BusinessUnitDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return businessUnitRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    public List<BusinessUnitDTO> findActiveByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return businessUnitRepository.findByCompanyIdAndActiveTrue(companyId).stream().map(this::toDTO).toList();
    }

    public BusinessUnitDTO findById(Long id) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        BusinessUnit unit = businessUnitRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Unidade de negócio não encontrada."));
        return toDTO(unit);
    }

    public void validateProductRestriction(Long restrictedBusinessUnitId, Product product) {
        if (restrictedBusinessUnitId == null) {
            return;
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

    public BusinessUnitDTO toDTO(BusinessUnit unit) {
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

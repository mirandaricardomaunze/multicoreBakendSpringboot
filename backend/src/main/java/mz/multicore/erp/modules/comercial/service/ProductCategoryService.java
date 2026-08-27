package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.comercial.dto.CreateProductCategoryRequest;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductCategoryService {

    private final ProductCategoryRepository repository;
    private final CompanyService companyService;

    public ProductCategoryService(ProductCategoryRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    @Transactional
    public ProductCategoryDTO create(CreateProductCategoryRequest req) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (repository.findByCodeAndCompaniesId(req.code(), companyId).isPresent()) {
            throw new BusinessRuleException("Já existe uma categoria com o código " + req.code() + ".");
        }
        ProductCategory shared = repository.findByCode(req.code()).orElse(null);
        if (shared != null) {
            shared.getCompanies().add(companyService.getCurrentCompanyReference(companyId));
            return toDTO(repository.save(shared));
        }
        ProductCategory c = new ProductCategory();
        c.setCode(req.code());
        c.setName(req.name());
        c.setColorHex(req.colorHex());
        c.setActive(true);
        c.setCreatedBy("SYSTEM");
        c.getCompanies().add(companyService.getCurrentCompanyReference(companyId));
        return toDTO(repository.save(c));
    }

    @Transactional
    public ProductCategoryDTO update(Long id, CreateProductCategoryRequest req) {
        ProductCategory c = repository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada."));
        c.setName(req.name());
        c.setColorHex(req.colorHex());
        return toDTO(repository.save(c));
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        ProductCategory c = repository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada."));
        c.setActive(active);
        repository.save(c);
    }

    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getAll() {
        return repository.findDistinctByCompaniesIdOrderByNameAsc(CurrentUserContext.getCurrentCompanyId())
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getActive() {
        return repository.findDistinctByCompaniesIdAndActiveTrueOrderByNameAsc(CurrentUserContext.getCurrentCompanyId())
                .stream().map(this::toDTO).toList();
    }

    private ProductCategoryDTO toDTO(ProductCategory c) {
        return new ProductCategoryDTO(c.getId(), c.getCode(), c.getName(), c.getColorHex(), c.isActive());
    }
}

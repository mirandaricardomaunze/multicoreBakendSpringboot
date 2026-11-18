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

/**
 * Product categories of the active company, with codes unique per company. {@link #ensureForCompany} only adds a
 * category whose code is missing, which is how activity profiles seed their starting categories without touching
 * existing ones.
 */
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

    /**
     * Makes the category available to the company without failing when it already is: links the shared
     * category with that code, or creates it. Used to seed the categories of an activity profile (SA-C01).
     *
     * @return true when the company gained the category, false when it already had it
     */
    @Transactional
    public boolean ensureForCompany(Long companyId, String code, String name, String colorHex) {
        if (repository.findByCodeAndCompaniesId(code, companyId).isPresent()) return false;
        ProductCategory category = repository.findByCode(code).orElseGet(() -> {
            ProductCategory c = new ProductCategory();
            c.setCode(code);
            c.setName(name);
            c.setColorHex(colorHex);
            c.setActive(true);
            c.setCreatedBy("SYSTEM");
            return c;
        });
        category.getCompanies().add(companyService.getCurrentCompanyReference(companyId));
        repository.save(category);
        return true;
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

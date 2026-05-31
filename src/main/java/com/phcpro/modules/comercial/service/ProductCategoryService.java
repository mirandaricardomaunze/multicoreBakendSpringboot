package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.comercial.dto.CreateProductCategoryRequest;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import com.phcpro.modules.comercial.model.ProductCategory;
import com.phcpro.modules.comercial.repository.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductCategoryService {

    private final ProductCategoryRepository repository;

    public ProductCategoryService(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductCategoryDTO create(CreateProductCategoryRequest req) {
        if (repository.findByCode(req.code()).isPresent()) {
            throw new BusinessRuleException("Já existe uma categoria com o código " + req.code() + ".");
        }
        ProductCategory c = new ProductCategory();
        c.setCode(req.code());
        c.setName(req.name());
        c.setColorHex(req.colorHex());
        c.setActive(true);
        c.setCreatedBy("SYSTEM");
        return toDTO(repository.save(c));
    }

    @Transactional
    public ProductCategoryDTO update(Long id, CreateProductCategoryRequest req) {
        ProductCategory c = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada."));
        c.setName(req.name());
        c.setColorHex(req.colorHex());
        return toDTO(repository.save(c));
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        ProductCategory c = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada."));
        c.setActive(active);
        repository.save(c);
    }

    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getActive() {
        return repository.findByActiveTrueOrderByNameAsc().stream().map(this::toDTO).toList();
    }

    private ProductCategoryDTO toDTO(ProductCategory c) {
        return new ProductCategoryDTO(c.getId(), c.getCode(), c.getName(), c.getColorHex(), c.isActive());
    }
}

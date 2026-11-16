package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comandos de escrita de produtos e categorias para os outros módulos (seeders, custo médio, unidades,
 * variantes, CRM, adiantamentos): gravam por aqui, nunca pelo {@link ProductRepository} nem pelo
 * {@link ProductCategoryRepository} (docs/MODULE_DECOUPLING_SPEC.md §2). Leitura: {@link ProductLookup}.
 */
@Service
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public ProductCommandService(ProductRepository productRepository, ProductCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public ProductCategory saveCategory(ProductCategory category) {
        return categoryRepository.save(category);
    }
}

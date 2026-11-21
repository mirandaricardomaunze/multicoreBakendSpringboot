package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Porta de leitura de produtos e categorias para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * É o único ponto pelo qual um módulo alheio lê {@link Product} ou {@link ProductCategory}; escritas
 * passam por {@link ProductCommandService}. As entidades devolvidas servem só para relações JPA dentro
 * do backend e nunca saem para um Controller.
 * <p>
 * Os métodos {@code find*} devolvem {@link Optional} para o consumidor que precisa de manter uma
 * mensagem específica já verificada por testes; os {@code require*} usam a mensagem normalizada.
 */
@Service
@Transactional(readOnly = true)
public class ProductLookup {

    static final String NOT_FOUND = "Produto não encontrado.";
    static final String CATEGORY_NOT_FOUND = "Categoria não encontrada.";

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public ProductLookup(ProductRepository productRepository, ProductCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // ---- Produtos -------------------------------------------------------------------------------

    /** Produto da empresa; {@code BusinessRuleException("Produto não encontrado.")} se não existir nela. */
    public Product requireForCompany(Long id, Long companyId) {
        return findForCompany(id, companyId).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Product> findForCompany(Long id, Long companyId) {
        return id == null ? Optional.empty() : productRepository.findByIdAndCompaniesId(id, companyId);
    }

    /** Produto pelo id, sem filtro de empresa (seeders, BOM, quebras herdadas). */
    public Product require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Product> find(Long id) {
        return id == null ? Optional.empty() : productRepository.findById(id);
    }

    /** Produtos da empresa ordenados por nome. */
    public List<Product> listForCompany(Long companyId) {
        return productRepository.findDistinctByCompaniesIdOrderByName(companyId);
    }

    public Optional<Product> findBySkuForCompany(String sku, Long companyId) {
        return sku == null ? Optional.empty() : productRepository.findBySkuAndCompaniesId(sku, companyId);
    }

    /** SKU é único na base (produto partilhado entre empresas). */
    public Optional<Product> findBySku(String sku) {
        return sku == null ? Optional.empty() : productRepository.findBySku(sku);
    }

    public Optional<Product> findByReferenceForCompany(String reference, Long companyId) {
        return reference == null ? Optional.empty()
                : productRepository.findByReferenceAndCompaniesId(reference, companyId);
    }

    public Optional<Product> findByBarcode(String barcode) {
        return barcode == null ? Optional.empty() : productRepository.findByBarcode(barcode);
    }

    public Optional<Product> findByBarcodeForCompany(String barcode, Long companyId) {
        return barcode == null ? Optional.empty()
                : productRepository.findByBarcodeAndCompaniesId(barcode, companyId);
    }

    /**
     * Artigos de qualquer empresa com algum destes SKU, códigos de barras ou referências (os três são únicos
     * na base), sem repetidos e por ordem de chegada. Conjunto vazio ou {@code null} não consulta a base.
     */
    public List<Product> findByAnyKey(Collection<String> skus, Collection<String> barcodes,
                                      Collection<String> references) {
        Map<Long, Product> found = new LinkedHashMap<>();
        Stream.of(
                        findByKeys(skus, productRepository::findBySkuIn),
                        findByKeys(barcodes, productRepository::findByBarcodeIn),
                        findByKeys(references, productRepository::findByReferenceIn))
                .flatMap(List::stream)
                .forEach(product -> found.putIfAbsent(product.getId(), product));
        return List.copyOf(found.values());
    }

    private static List<Product> findByKeys(Collection<String> keys,
                                            Function<Collection<String>, List<Product>> query) {
        return keys == null || keys.isEmpty() ? List.of() : query.apply(keys);
    }

    public List<Product> findAllById(Collection<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : productRepository.findAllById(ids);
    }

    public long countByBusinessUnit(Long businessUnitId) {
        return productRepository.countByBusinessUnitId(businessUnitId);
    }

    // ---- Categorias -----------------------------------------------------------------------------

    /** Categoria da empresa; {@code BusinessRuleException("Categoria não encontrada.")} se não existir nela. */
    public ProductCategory requireCategoryForCompany(Long id, Long companyId) {
        return findCategoryForCompany(id, companyId)
                .orElseThrow(() -> new BusinessRuleException(CATEGORY_NOT_FOUND));
    }

    public Optional<ProductCategory> findCategoryForCompany(Long id, Long companyId) {
        return id == null ? Optional.empty() : categoryRepository.findByIdAndCompaniesId(id, companyId);
    }

    /** Categorias da empresa (activas e inactivas), por nome. */
    public List<ProductCategory> listCategoriesForCompany(Long companyId) {
        return companyId == null ? List.of() : categoryRepository.findDistinctByCompaniesIdOrderByNameAsc(companyId);
    }

    public List<ProductCategory> findAllCategories() {
        return categoryRepository.findAll();
    }

    public long countCategories() {
        return categoryRepository.count();
    }
}

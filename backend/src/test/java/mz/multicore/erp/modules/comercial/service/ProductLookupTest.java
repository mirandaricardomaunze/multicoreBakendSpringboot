package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-P01 — porta de leitura de produtos e categorias para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class ProductLookupTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final ProductCategoryRepository categories = mock(ProductCategoryRepository.class);
    private final ProductLookup lookup = new ProductLookup(products, categories);

    private static Product product(long id) {
        Product p = new Product();
        p.setId(id);
        return p;
    }

    private static ProductCategory category(long id) {
        ProductCategory c = new ProductCategory();
        c.setId(id);
        return c;
    }

    @Test
    void requireForCompany_existente_devolveProduto() {
        Product p = product(7L);
        when(products.findByIdAndCompaniesId(7L, 1L)).thenReturn(Optional.of(p));

        assertThat(lookup.requireForCompany(7L, 1L)).isSameAs(p);
    }

    @Test
    void requireForCompany_inexistente_mensagemNormalizada() {
        when(products.findByIdAndCompaniesId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.requireForCompany(9L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Produto não encontrado.");
    }

    @Test
    void requireForCompany_idNulo_recusaSemTocarNoRepository() {
        assertThatThrownBy(() -> lookup.requireForCompany(null, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Produto não encontrado.");
        verifyNoInteractions(products);
    }

    @Test
    void findForCompany_e_find_devolvemOptional() {
        Product p = product(4L);
        when(products.findByIdAndCompaniesId(4L, 2L)).thenReturn(Optional.of(p));
        when(products.findById(4L)).thenReturn(Optional.of(p));

        assertThat(lookup.findForCompany(4L, 2L)).contains(p);
        assertThat(lookup.findForCompany(null, 2L)).isEmpty();
        assertThat(lookup.find(4L)).contains(p);
        assertThat(lookup.find(null)).isEmpty();
    }

    @Test
    void require_semEmpresa_mensagemNormalizada() {
        when(products.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.require(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Produto não encontrado.");
    }

    @Test
    void listForCompany_delega() {
        List<Product> all = List.of(product(1L), product(2L));
        when(products.findDistinctByCompaniesIdOrderByName(3L)).thenReturn(all);

        assertThat(lookup.listForCompany(3L)).isEqualTo(all);
    }

    @Test
    void pesquisasPorCodigo_delegam() {
        Product p = product(6L);
        when(products.findBySkuAndCompaniesId("SKU-1", 1L)).thenReturn(Optional.of(p));
        when(products.findBySku("SKU-1")).thenReturn(Optional.of(p));
        when(products.findByReferenceAndCompaniesId("REF-1", 1L)).thenReturn(Optional.of(p));
        when(products.findByBarcode("560123")).thenReturn(Optional.of(p));
        when(products.findByBarcodeAndCompaniesId("560123", 1L)).thenReturn(Optional.of(p));

        assertThat(lookup.findBySkuForCompany("SKU-1", 1L)).contains(p);
        assertThat(lookup.findBySku("SKU-1")).contains(p);
        assertThat(lookup.findByReferenceForCompany("REF-1", 1L)).contains(p);
        assertThat(lookup.findByBarcode("560123")).contains(p);
        assertThat(lookup.findByBarcodeForCompany("560123", 1L)).contains(p);
    }

    @Test
    void pesquisasPorCodigoNulo_naoTocamNoRepository() {
        assertThat(lookup.findBySkuForCompany(null, 1L)).isEmpty();
        assertThat(lookup.findBySku(null)).isEmpty();
        assertThat(lookup.findByReferenceForCompany(null, 1L)).isEmpty();
        assertThat(lookup.findByBarcode(null)).isEmpty();
        assertThat(lookup.findByBarcodeForCompany(null, 1L)).isEmpty();
        verifyNoInteractions(products);
    }

    @Test
    void findAllById_e_countByBusinessUnit_delegam() {
        List<Product> found = List.of(product(1L));
        when(products.findAllById(Set.of(1L))).thenReturn(found);
        when(products.countByBusinessUnitId(8L)).thenReturn(3L);

        assertThat(lookup.findAllById(Set.of(1L))).isEqualTo(found);
        assertThat(lookup.findAllById(List.of())).isEmpty();
        assertThat(lookup.countByBusinessUnit(8L)).isEqualTo(3L);
    }

    @Test
    void categorias_requireForCompany_eMensagemNormalizada() {
        ProductCategory c = category(11L);
        when(categories.findByIdAndCompaniesId(11L, 1L)).thenReturn(Optional.of(c));
        when(categories.findByIdAndCompaniesId(12L, 1L)).thenReturn(Optional.empty());

        assertThat(lookup.requireCategoryForCompany(11L, 1L)).isSameAs(c);
        assertThat(lookup.findCategoryForCompany(11L, 1L)).contains(c);
        assertThat(lookup.findCategoryForCompany(null, 1L)).isEmpty();
        assertThatThrownBy(() -> lookup.requireCategoryForCompany(12L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Categoria não encontrada.");
    }

    @Test
    void categorias_findAll_e_count_delegam() {
        List<ProductCategory> all = List.of(category(1L), category(2L));
        when(categories.findAll()).thenReturn(all);
        when(categories.count()).thenReturn(2L);

        assertThat(lookup.findAllCategories()).isEqualTo(all);
        assertThat(lookup.countCategories()).isEqualTo(2L);
    }

    @Test
    void contrato_serviceSoDeLeitura() {
        assertThat(ProductLookup.class).hasAnnotation(Service.class);
        Transactional tx = ProductLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
    }

    @Test
    void escritaAlheia_passaPelosComandosDoServiceDeProduto() {
        Product p = product(10L);
        ProductCategory c = category(20L);
        when(products.save(p)).thenReturn(p);
        when(categories.save(c)).thenReturn(c);
        ProductCommandService commands = new ProductCommandService(products, categories);

        assertThat(commands.saveProduct(p)).isSameAs(p);
        assertThat(commands.saveCategory(c)).isSameAs(c);
        verify(products).save(p);
        verify(categories).save(c);
        assertThat(ProductCommandService.class).hasAnnotation(Service.class);
        Transactional tx = ProductCommandService.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }
}

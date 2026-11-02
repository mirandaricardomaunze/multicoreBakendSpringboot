package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class LineRowMapperTest {

    @Test
    void map_transportsProductPackagingCompositionToDocumentRow() {
        Product product = new Product();
        product.setName("Iogurte");
        product.setPackagesPerBox(12);
        product.setUnitsPerPackage(6);

        LineItemsTableRenderer.Row row = new LineRowMapper(mock(ProductBatchRepository.class)).map(
                product, null, new BigDecimal("18"), new BigDecimal("100"),
                new BigDecimal("0.16"), BigDecimal.ZERO, new BigDecimal("2088"));

        assertEquals(12, row.packagesPerBox());
        assertEquals(6, row.unitsPerPackage());
        assertEquals("3", LineItemsTableRenderer.formatPackages(row));
        assertEquals("25%", LineItemsTableRenderer.formatBoxPercentage(row));
    }

    @Test
    @DisplayName("Caixa com 4 embalagens e 3 vendidas exibe 3 emb. e 0.75 cx. na fatura/encomenda")
    void map_modelTablePackaging_partialBoxShowsDecimalPercentage() {
        Product product = new Product();
        product.setName("Óleo Alimentar 1L");
        product.setPackagesPerBox(4);
        product.setUnitsPerPackage(1);

        LineItemsTableRenderer.Row row = new LineRowMapper(mock(ProductBatchRepository.class)).map(
                product, null, new BigDecimal("3"), new BigDecimal("150"),
                new BigDecimal("0.16"), BigDecimal.ZERO, new BigDecimal("450"));

        assertEquals("3", LineItemsTableRenderer.formatModelPackages(row));
        assertEquals("0,75", LineItemsTableRenderer.formatModelBoxes(row));
    }

    @Test
    @DisplayName("Caixa completa exibe número de embalagens e 1 caixa inteira")
    void map_modelTablePackaging_fullBoxShowsInteger() {
        Product product = new Product();
        product.setName("Biscoitos 200g");
        product.setPackagesPerBox(4);
        product.setUnitsPerPackage(1);

        LineItemsTableRenderer.Row row = new LineRowMapper(mock(ProductBatchRepository.class)).map(
                product, null, new BigDecimal("4"), new BigDecimal("50"),
                new BigDecimal("0.16"), BigDecimal.ZERO, new BigDecimal("200"));

        assertEquals("4", LineItemsTableRenderer.formatModelPackages(row));
        assertEquals("1", LineItemsTableRenderer.formatModelBoxes(row));
    }

    @Test
    @DisplayName("Produto sem composição de embalagem comercial exibe 0 e 0.00")
    void map_modelTablePackaging_unitProductShowsZero() {
        Product product = new Product();
        product.setName("Serviço de Consultoria");
        product.setPackagesPerBox(1);
        product.setUnitsPerPackage(1);

        LineItemsTableRenderer.Row row = new LineRowMapper(mock(ProductBatchRepository.class)).map(
                product, null, new BigDecimal("1"), new BigDecimal("5000"),
                new BigDecimal("0.16"), BigDecimal.ZERO, new BigDecimal("5000"));

        assertEquals("0", LineItemsTableRenderer.formatModelPackages(row));
        assertEquals("0,00", LineItemsTableRenderer.formatModelBoxes(row));
    }
}

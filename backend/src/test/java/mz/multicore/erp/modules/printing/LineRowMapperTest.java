package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
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
}

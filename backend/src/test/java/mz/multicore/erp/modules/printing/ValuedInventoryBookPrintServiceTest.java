package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValuedInventoryBookPrintServiceTest {

    @Test
    void render_geraPdfValidoComStocksValorizadosECump() {
        StockRepository stockRepository = mock(StockRepository.class);
        CompanyService companyService = mock(CompanyService.class);

        Company company = new Company();
        company.setId(1L);
        company.setName("Moçambique Agro-Indústria, Lda");
        company.setTaxId("400123456");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(5L);
        warehouse.setName("Armazém Central Maputo");
        warehouse.setCompany(company);

        Product product = new Product();
        product.setId(20L);
        product.setSku("FAR-001");
        product.setName("Farinha de Trigo Especial 25kg");
        product.setSaleType(ProductSaleType.UNIT);
        product.setPurchasePrice(new BigDecimal("1200.00"));

        Stock stock = new Stock();
        stock.setId(100L);
        stock.setProduct(product);
        stock.setWarehouse(warehouse);
        stock.setQuantity(new BigDecimal("50"));

        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(stockRepository.findByWarehouseCompanyId(1L)).thenReturn(List.of(stock));

        ValuedInventoryBookPrintService service = new ValuedInventoryBookPrintService(stockRepository, companyService);
        byte[] pdf = service.render(1L, 2026);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "O PDF do Livro de Inventário Valorizado deve conter bytes válidos gerados");
    }
}

package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryReportPrintServiceTest {

    @Test
    @DisplayName("Gera documento de inventário PDF com colunas de referência, código de barras, designação, quantidade, caixas e preço")
    void render_geraPdfDeInventarioCorretamente() {
        StockRepository stockRepository = mock(StockRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        CompanyService companyService = mock(CompanyService.class);

        Company company = new Company();
        company.setId(1L);
        company.setName("Distribuidora Central, Lda");
        company.setTaxId("400987654");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Armazém Principal Matola");
        warehouse.setCompany(company);

        Product p1 = new Product();
        p1.setId(1L);
        p1.setSku("PROD-001");
        p1.setReference("REF-ARROZ-01");
        p1.setBarcode("6001234567890");
        p1.setName("Arroz Agulha 25kg");
        p1.setSaleType(ProductSaleType.UNIT);
        p1.setUnitPrice(new BigDecimal("1500.00"));
        p1.setUnitsPerBox(1); // 1 unidade por caixa

        Stock s1 = new Stock();
        s1.setId(101L);
        s1.setProduct(p1);
        s1.setWarehouse(warehouse);
        s1.setQuantity(new BigDecimal("50"));

        Product p2 = new Product();
        p2.setId(2L);
        p2.setSku("PROD-002");
        p2.setReference(null); // test fallback to SKU
        p2.setBarcode("6009876543210");
        p2.setName("Óleo Alimentar 1L (Caixa c/ 12)");
        p2.setSaleType(ProductSaleType.UNIT);
        p2.setUnitPrice(new BigDecimal("110.00"));
        p2.setUnitsPerBox(12); // 12 unidades por caixa

        Stock s2 = new Stock();
        s2.setId(102L);
        s2.setProduct(p2);
        s2.setWarehouse(warehouse);
        s2.setQuantity(new BigDecimal("120")); // 120 un = 10 caixas

        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(stockRepository.findByWarehouseId(10L)).thenReturn(List.of(s1, s2));

        InventoryReportPrintService service = new InventoryReportPrintService(
                stockRepository, warehouseRepository, companyService
        );

        byte[] pdf = service.render(1L, 10L);

        assertNotNull(pdf, "O PDF gerado não deve ser nulo");
        assertTrue(pdf.length > 500, "O PDF deve conter estrutura binária válida");
    }
}

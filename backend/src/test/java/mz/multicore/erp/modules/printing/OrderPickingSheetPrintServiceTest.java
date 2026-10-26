package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderPickingSheetPrintServiceTest {

    private OrderRepository orderRepository;
    private OrderPickingSheetPrintService service;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        service = new OrderPickingSheetPrintService(orderRepository);
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("Renderiza Folha de Separação A4 executiva com colunas de caixas e visto óptico")
    void renderA4_generatesValidPdfDocument() throws IOException {
        Company company = new Company();
        company.setId(1L);
        company.setName("Distribuidora Moçambique Lda");
        company.setTaxId("400123456");

        Client client = new Client();
        client.setName("Supermercado Central");
        client.setTaxId("400987654");

        Warehouse warehouse = new Warehouse();
        warehouse.setName("Depósito Principal");

        Product p1 = new Product();
        p1.setName("Arroz 25kg");
        p1.setReference("ARR-25");
        p1.setBarcode("6001234567890");
        p1.setPackagesPerBox(4);
        p1.setUnitsPerPackage(1);

        OrderLine line1 = new OrderLine();
        line1.setProduct(p1);
        line1.setQuantity(new BigDecimal("12")); // 3 caixas
        line1.setBatchNumber("LOT-2026-A");

        Product p2 = new Product();
        p2.setName("Sabão Avulso");
        p2.setReference("SAB-01");
        p2.setBarcode("6009876543210");
        p2.setPackagesPerBox(1);
        p2.setUnitsPerPackage(1);

        OrderLine line2 = new OrderLine();
        line2.setProduct(p2);
        line2.setQuantity(new BigDecimal("5")); // sem caixa -> 0.00
        line2.setBatchNumber("LOT-2026-B");

        Order order = new Order();
        order.setId(100L);
        order.setOrderNumber("ENC-2026-0001");
        order.setStatus("IN_SEPARATION");
        order.setCompany(company);
        order.setClient(client);
        order.setWarehouse(warehouse);
        order.getLines().add(line1);
        order.getLines().add(line2);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        byte[] pdfBytes = service.renderA4(100L, false);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        PdfReader reader = new PdfReader(pdfBytes);
        assertTrue(reader.getNumberOfPages() >= 1);
        reader.close();
    }
}

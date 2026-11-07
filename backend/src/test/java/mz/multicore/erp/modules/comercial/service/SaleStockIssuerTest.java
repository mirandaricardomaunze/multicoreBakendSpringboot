package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** POS, fatura directa e cotação convertida também respeitam reservas (RS-08, RS-09). */
class SaleStockIssuerTest {

    private InventoryService inventory;
    private OrderLineRepository orderLines;
    private SaleStockIssuer issuer;
    private Warehouse warehouse;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        inventory = mock(InventoryService.class);
        orderLines = mock(OrderLineRepository.class);
        issuer = new SaleStockIssuer(inventory, orderLines);
        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Loja");
        Product product = new Product();
        product.setId(1L);
        product.setName("Óleo 1L");
        InvoiceLine line = new InvoiceLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("4"));
        invoice = new Invoice();
        invoice.addLine(line);
        when(inventory.lockAndGetPhysicalQuantity(1L, 10L)).thenReturn(new BigDecimal("10"));
    }

    @Test // RS-08
    void vendaAoBalcao_naoLevaStockSeparadoParaOutroCliente() {
        when(orderLines.sumActiveReservations(1L, 10L)).thenReturn(new BigDecimal("8"));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> issuer.issue(invoice, warehouse, "Venda POS"));

        assertTrue(error.getMessage().contains("Óleo 1L"), error.getMessage());
        verify(inventory, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test // RS-09
    void stockLivre_registaASaidaDeCadaLinha() {
        when(orderLines.sumActiveReservations(1L, 10L)).thenReturn(new BigDecimal("6"));

        issuer.issue(invoice, warehouse, "Venda POS");

        verify(inventory).registerMovement(any(), eq(warehouse), eq(new BigDecimal("-4")), eq("SALE"), any(), any(), eq("Venda POS"));
    }
}

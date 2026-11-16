package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Saída de stock respeita reservas de outros pedidos — ver docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md (RS-*). */
class OrderStockAvailabilityTest {

    private InventoryService inventory;
    private OrderLineRepository lines;
    private Warehouse warehouse;
    private Product arroz;

    @BeforeEach
    void setUp() {
        inventory = mock(InventoryService.class);
        lines = mock(OrderLineRepository.class);
        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Central");
        arroz = new Product();
        arroz.setId(1L);
        arroz.setName("Arroz");
        when(inventory.lockAndGetPhysicalQuantity(1L, 10L)).thenReturn(new BigDecimal("10"));
    }

    private static OrderStockAvailability.Outgoing out(Product product, String quantity) {
        return new OrderStockAvailability.Outgoing(product, new BigDecimal(quantity));
    }

    @Test // RS-01
    void encomendaA4_naoLevaStockReservadoParaOutroPedido() {
        when(lines.sumActiveReservations(1L, 10L)).thenReturn(new BigDecimal("8"));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> OrderStockAvailability.require(new Order(), warehouse, List.of(out(arroz, "3")), inventory, lines));

        assertTrue(error.getMessage().contains("Arroz"), error.getMessage());
        assertTrue(error.getMessage().contains("reservado"), error.getMessage());
    }

    @Test // RS-02
    void stockLivreSuficiente_passa() {
        when(lines.sumActiveReservations(1L, 10L)).thenReturn(new BigDecimal("7"));

        assertDoesNotThrow(() -> OrderStockAvailability.require(new Order(), warehouse, List.of(out(arroz, "3")), inventory, lines));
    }

    @Test // RS-03
    void pedidoDeSeparacao_consomeAPropriaReserva() {
        // As 8 reservadas são deste pedido: não contam contra ele.
        when(lines.sumActiveReservations(1L, 10L)).thenReturn(new BigDecimal("8"));
        Order order = new Order();
        order.setWarehouse(warehouse);
        order.setReservationActive(true);
        OrderLine line = new OrderLine();
        line.setProduct(arroz);
        line.setReservedQuantity(new BigDecimal("8"));
        order.addLine(line);

        assertDoesNotThrow(() -> OrderStockAvailability.require(order, warehouse, List.of(out(arroz, "8")), inventory, lines));
    }

    @Test // RS-04
    void duasLinhasDoMesmoArtigo_somam() {
        // Cada linha de 6 cabia sozinha nas 10; juntas são 12.
        when(lines.sumActiveReservations(1L, 10L)).thenReturn(BigDecimal.ZERO);

        assertThrows(BusinessRuleException.class, () -> OrderStockAvailability.require(
                new Order(), warehouse, List.of(out(arroz, "6"), out(arroz, "6")), inventory, lines));
    }
}

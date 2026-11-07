package mz.multicore.erp.modules.comercial.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Estados da encomenda num só enum — ver docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md (EN-*). */
class OrderStatusTest {

    @Test // EN-01
    void faturadaPeloCircuitoDeSeparacao_naoFicaEmAtraso() {
        // INVOICED faltava na lista escrita à mão: pedidos de separação faturados apareciam em atraso.
        for (String closed : new String[]{"BILLED", "INVOICED", "GUIDED", "TRANSFERRED", "CANCELLED"}) {
            Order order = new Order();
            order.setStatus(closed);
            order.setExpectedDeliveryDate(LocalDate.of(2026, 1, 1));
            assertFalse(order.isDeliveryOverdue(LocalDate.of(2026, 2, 1)), closed);
        }
    }

    @Test // EN-02
    void encomendaAbertaDepoisDaData_ficaEmAtraso() {
        Order order = new Order();
        order.setStatus("SEPARATED");
        order.setExpectedDeliveryDate(LocalDate.of(2026, 1, 1));
        assertTrue(order.isDeliveryOverdue(LocalDate.of(2026, 2, 1)));
    }

    @Test // EN-03
    void billedEInvoiced_saoAmbosFaturados() {
        assertTrue(OrderStatus.isBilled("BILLED"));
        assertTrue(OrderStatus.isBilled("invoiced"));
        assertFalse(OrderStatus.isBilled("SEPARATED"));
    }

    @Test // EN-04
    void cancelavelSoAntesDeGuiaTransferenciaOuFatura() {
        assertTrue(OrderStatus.isCancellable("SEPARATED"));
        assertFalse(OrderStatus.isCancellable("GUIDE_PENDING"));
        assertFalse(OrderStatus.isCancellable("TRANSFER_PENDING"));
        assertFalse(OrderStatus.isCancellable("INVOICED"));
    }

    @Test // EN-05
    void estadoDesconhecidoOuVazio_naoRebenta() {
        assertNull(OrderStatus.parse("QUALQUER"));
        assertNull(OrderStatus.parse(null));
        assertFalse(OrderStatus.is(null, OrderStatus.PENDING));
        assertFalse(OrderStatus.isClosed("   "));
    }

    @Test // EN-06
    void todosOsEstadosTemRotuloEmPortugues() {
        for (OrderStatus status : OrderStatus.values()) {
            assertNotEquals(status.name(), OrderStatusLabel.of(status.name()), status.name());
        }
    }
}

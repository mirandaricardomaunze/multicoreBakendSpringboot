package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ED-01..05 e ED-08 — a via como facto do documento.
 *
 * <p>Ver docs/ENCOMENDA_DUAS_VIAS_SPEC.md. O que estas asserções protegem é a ligação entre a via
 * e as três decisões que dela dependem: aprovação, formato do documento e circuito de separação.
 */
class OrderKindTest {

    @Test // ED-01 / ED-02
    void aprovacaoEExigidaSoNaEncomendaA4() {
        assertTrue(OrderKind.FORMAL_ORDER.requiresApproval());
        assertFalse(OrderKind.PICKING_REQUEST.requiresApproval());
    }

    @Test // ED-03
    void oTalaoTermicoEDoPedidoDeSeparacao() {
        assertTrue(OrderKind.PICKING_REQUEST.isThermal());
        assertFalse(OrderKind.FORMAL_ORDER.isThermal());
        assertTrue(OrderKind.PICKING_REQUEST.usesSeparationFlow());
        assertFalse(OrderKind.FORMAL_ORDER.usesSeparationFlow());
    }

    @Test // ED-04
    void osRotulosSaoEmPortuguesENaoCodigosInternos() {
        for (OrderKind kind : OrderKind.values()) {
            assertFalse(kind.label().contains("_"), "rótulo com código interno: " + kind.label());
            assertNotEquals(kind.name(), kind.label());
        }
        assertEquals("Encomenda (A4)", OrderKind.FORMAL_ORDER.label());
        assertEquals("Pedido de separação", OrderKind.PICKING_REQUEST.label());
    }

    @Test // ED-05
    void semViaDeclaradaAEncomendaEA4() {
        assertEquals(OrderKind.FORMAL_ORDER, OrderKind.orDefault(null));
        assertEquals(OrderKind.PICKING_REQUEST, OrderKind.orDefault(OrderKind.PICKING_REQUEST));
    }

    @Test // ED-08
    void oConstrutorAntigoDoPedidoContinuaAFuncionar() {
        // Retrocompatibilidade: quem construía o pedido antes da via existir continua a compilar
        // e cai na via que o sistema sempre teve na porta comercial.
        CreateOrderRequest request = new CreateOrderRequest(1L, null, 2L, 3L, List.of());

        assertEquals(OrderKind.FORMAL_ORDER, request.effectiveKind());
    }

    @Test // ED-05 (fronteira HTTP)
    void pedidoSemViaExplicitaResolveParaA4() {
        CreateOrderRequest request = new CreateOrderRequest(1L, null, 2L, 3L, List.of(), null);

        assertEquals(OrderKind.FORMAL_ORDER, request.effectiveKind());
    }

    @Test // ED-06 (a via é gravada no documento)
    void aEncomendaNasceComViaNaoNula() {
        assertNotNull(new Order().getKind(), "uma encomenda sem via não deve existir");
        assertEquals(OrderKind.FORMAL_ORDER, new Order().getKind());
    }
}

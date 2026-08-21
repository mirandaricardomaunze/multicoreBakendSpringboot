package mz.multicore.erp.modules.comercial.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RI-01..06 — a via da reposição interna e, sobretudo, a trava que ela carrega.
 *
 * <p>Ver docs/REPOSICAO_INTERNA_SPEC.md §3. {@code isBillable()} não é uma conveniência de leitura:
 * é o que impede o stock de sair duas vezes pela mesma mercadoria.
 */
class InternalReplenishmentKindTest {

    @Test // RI-01
    void aReposicaoInternaNaoPassaPorAprovacao() {
        // Quem aprova é a transferência, que já exige gerente ou administrador para mover stock.
        assertFalse(OrderKind.INTERNAL_REPLENISHMENT.requiresApproval());
    }

    @Test // RI-02 / RI-03
    void aReposicaoInternaETrabalhoDeArmazem() {
        assertTrue(OrderKind.INTERNAL_REPLENISHMENT.isThermal());
        assertTrue(OrderKind.INTERNAL_REPLENISHMENT.usesSeparationFlow());
    }

    @Test // RI-04 / RI-05 💰
    void soAsViasDeVendaSaoFacturaveis() {
        assertFalse(OrderKind.INTERNAL_REPLENISHMENT.isBillable(),
                "facturar uma reposição interna faria o stock sair duas vezes");
        assertTrue(OrderKind.FORMAL_ORDER.isBillable());
        assertTrue(OrderKind.PICKING_REQUEST.isBillable());
    }

    @Test // RI-04 (a outra ponta da trava)
    void soAReposicaoInternaViraTransferencia() {
        assertTrue(OrderKind.INTERNAL_REPLENISHMENT.usesWarehouseTransfer());
        assertFalse(OrderKind.FORMAL_ORDER.usesWarehouseTransfer());
        assertFalse(OrderKind.PICKING_REQUEST.usesWarehouseTransfer());
    }

    @Test // RI-07 (a via é que exige o destino)
    void soAReposicaoInternaExigeArmazemDeDestino() {
        assertTrue(OrderKind.INTERNAL_REPLENISHMENT.requiresDestinationWarehouse());
        assertFalse(OrderKind.FORMAL_ORDER.requiresDestinationWarehouse());
        assertFalse(OrderKind.PICKING_REQUEST.requiresDestinationWarehouse());
    }

    @Test // RI-06
    void oRotuloEEmPortuguesENaoUmCodigoInterno() {
        assertEquals("Reposição interna", OrderKind.INTERNAL_REPLENISHMENT.label());
        for (OrderKind kind : OrderKind.values()) {
            assertFalse(kind.label().contains("_"), "rótulo com código interno: " + kind.label());
        }
    }

    @Test // as vias de venda não mudaram
    void asViasQueJaExistiamContinuamComoEstavam() {
        assertTrue(OrderKind.FORMAL_ORDER.requiresApproval());
        assertFalse(OrderKind.FORMAL_ORDER.isThermal());
        assertFalse(OrderKind.FORMAL_ORDER.usesSeparationFlow());
        assertFalse(OrderKind.PICKING_REQUEST.requiresApproval());
        assertTrue(OrderKind.PICKING_REQUEST.isThermal());
        assertTrue(OrderKind.PICKING_REQUEST.usesSeparationFlow());
    }
}

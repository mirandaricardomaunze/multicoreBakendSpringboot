package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollBar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes das acções de scroll da {@link TableNavigator} sobre uma {@link JScrollBar} com modelo
 * conhecido (sem display). Molde: {@code TableFilterTest}. Cenários TN-01..TN-07.
 */
class TableNavigatorTest {

    /** value=50, extent=20, min=0, max=200 → limite superior efectivo do value = 180. */
    private static JScrollBar bar() {
        return new JScrollBar(JScrollBar.VERTICAL, 50, 20, 0, 200);
    }

    @Test // TN-01
    void top_vaiParaOMinimo() {
        JScrollBar bar = bar();
        TableNavigator.top(bar);
        assertEquals(0, bar.getValue());
    }

    @Test // TN-02
    void bottom_vaiParaOFundo_comClampDoExtent() {
        JScrollBar bar = bar();
        TableNavigator.bottom(bar);
        assertEquals(180, bar.getValue()); // maximum(200) - extent(20)
    }

    @Test // TN-03
    void pageDown_desceUmaPagina() {
        JScrollBar bar = bar();
        TableNavigator.pageDown(bar);
        assertEquals(70, bar.getValue()); // 50 + visibleAmount(20)
    }

    @Test // TN-04
    void pageUp_sobeUmaPagina() {
        JScrollBar bar = bar();
        bar.setValue(70);
        TableNavigator.pageUp(bar);
        assertEquals(50, bar.getValue());
    }

    @Test // TN-05
    void pageUp_noTopo_ficaNoTopo() {
        JScrollBar bar = bar();
        bar.setValue(0);
        TableNavigator.pageUp(bar);
        assertEquals(0, bar.getValue());
    }

    @Test // TN-06
    void pageDown_noFundo_ficaNoFundo() {
        JScrollBar bar = bar();
        bar.setValue(180);
        TableNavigator.pageDown(bar);
        assertEquals(180, bar.getValue());
    }

    @Test // TN-07
    void helpers_comNull_naoLancam() {
        assertDoesNotThrow(() -> {
            TableNavigator.top(null);
            TableNavigator.bottom(null);
            TableNavigator.pageUp(null);
            TableNavigator.pageDown(null);
        });
    }

    @Test // UX-01
    void overflowed_quandoConteudoTransborda() {
        assertTrue(TableNavigator.overflowed(0, 200, 20));
    }

    @Test // UX-02
    void overflowed_quandoConteudoCabe() {
        assertFalse(TableNavigator.overflowed(0, 20, 20)); // extent >= amplitude
        assertFalse(TableNavigator.overflowed(0, 15, 20));
    }
}

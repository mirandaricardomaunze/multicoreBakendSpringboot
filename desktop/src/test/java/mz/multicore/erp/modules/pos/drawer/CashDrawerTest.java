package mz.multicore.erp.modules.pos.drawer;

import mz.multicore.erp.desktop.config.DesktopLocalSettings.DrawerSettings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Cash drawer: the ESC/POS pulse and when a sale opens the drawer. */
class CashDrawerTest {

    @Test
    void pulseIsEscPOnTheChosenPin() {
        assertThat(CashDrawer.pulse(0)).containsExactly(0x1B, 0x70, 0x00, 0x19, 0xFA);
        assertThat(CashDrawer.pulse(1)).containsExactly(0x1B, 0x70, 0x01, 0x19, 0xFA);
    }

    @Test
    void onlyCashSalesOpenAConfiguredDrawer() {
        DrawerSettings on = new DrawerSettings(true, "EPSON TM-T20", 0);
        assertThat(CashDrawer.opensFor("CASH", on)).isTrue();
        assertThat(CashDrawer.opensFor("CARD", on)).isFalse();
        assertThat(CashDrawer.opensFor("CREDIT", on)).isFalse();
        assertThat(CashDrawer.opensFor("CASH", new DrawerSettings(false, "EPSON TM-T20", 0))).isFalse();
        assertThat(CashDrawer.opensFor("CASH", new DrawerSettings(true, "", 0))).isFalse();
    }
}

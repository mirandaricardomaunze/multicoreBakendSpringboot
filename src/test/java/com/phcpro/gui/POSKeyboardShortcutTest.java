package com.phcpro.gui;

import org.junit.jupiter.api.Test;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class POSKeyboardShortcutTest {

    @Test
    void bindsShortcutToExecutableAction() {
        InputMap input = new InputMap();
        ActionMap actions = new ActionMap();
        AtomicBoolean executed = new AtomicBoolean();
        KeyStroke f9 = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);

        POSPanel.bindShortcut(input, actions, "checkout", f9, () -> executed.set(true));

        assertEquals("checkout", input.get(f9));
        assertNotNull(actions.get("checkout"));
        actions.get("checkout").actionPerformed(null);
        assertTrue(executed.get());
    }

    @Test
    void missingProductRateUsesSameStandardVatAsCheckout() {
        assertEquals(new BigDecimal("0.16"), POSPanel.effectiveTaxRate(null));
        assertEquals(new BigDecimal("0.05"), POSPanel.effectiveTaxRate(new BigDecimal("0.05")));
    }
}

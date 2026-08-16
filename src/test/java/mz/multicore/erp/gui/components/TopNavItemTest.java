package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopNavItemTest {

    @Test
    void exposesAccessibleNameAndSupportsKeyboardActivation() throws Exception {
        AtomicInteger activations = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            TopNavItem item = new TopNavItem(null, "Configurações", UIHelper.MODULE_CONFIG,
                    activations::incrementAndGet);

            assertTrue(item.isFocusable());
            assertEquals("Configurações", item.getAccessibleContext().getAccessibleName());

            Action action = item.getActionMap().get("activate");
            action.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, "activate"));
        });

        assertEquals(1, activations.get());
    }
}

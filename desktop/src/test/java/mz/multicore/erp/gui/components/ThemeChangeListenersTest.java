package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Quem não é reconstruído com a janela principal fica a saber da troca de tema (ET-04). */
class ThemeChangeListenersTest {

    @Test
    void avisaNoEdtEDeixaDeAvisarDepoisDeRemovido() throws Exception {
        List<Theme> received = new ArrayList<>();
        Consumer<Theme> listener = received::add;
        int before = ThemeChangeListeners.count();

        ThemeChangeListeners.add(listener);
        ThemeChangeListeners.fire(Theme.DARK);
        EventQueue.invokeAndWait(() -> { });
        ThemeChangeListeners.remove(listener);
        ThemeChangeListeners.fire(Theme.LIGHT);
        EventQueue.invokeAndWait(() -> { });

        assertEquals(List.of(Theme.DARK), received);
        assertEquals(before, ThemeChangeListeners.count());
    }
}

package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionMenuButtonTest {

    @Test
    void nomeVazio_eRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> new ActionMenuButton(" "));
    }

    @Test
    void configuraDimensaoEAcessibilidadeCanonicas() {
        ActionMenuButton button = UIHelper.createActionMenuButton("Mais acções");

        assertEquals(UIHelper.FORM_CONTROL_HEIGHT, button.getPreferredSize().height);
        assertEquals("Mais acções", button.getToolTipText());
        assertEquals("Mais acções", button.getAccessibleContext().getAccessibleName());
        assertNotNull(button.getIcon());
    }

    @Test
    void preservaEntradasEExecutaListenerUmaVez() {
        AtomicInteger calls = new AtomicInteger();
        Icon icon = UIHelper.icon("fas-print", 14);
        ActionMenuButton button = UIHelper.createActionMenuButton("Documentos")
                .addAction("Imprimir PDF", icon, calls::incrementAndGet)
                .addAction("Exportar Lista", UIHelper.icon("fas-file-pdf", 14), () -> {});

        assertEquals(2, button.actionCount());
        assertEquals("Imprimir PDF", button.actionAt(0).getText());
        assertEquals(icon, button.actionAt(0).getIcon());
        button.actionAt(0).doClick();
        assertEquals(1, calls.get());
    }

    @Test
    void limitaMenuACincoEntradas() {
        ActionMenuButton button = UIHelper.createActionMenuButton("Mais acções");
        for (int i = 1; i <= 5; i++) {
            button.addAction("Acção " + i, null, () -> {});
        }

        assertThrows(IllegalStateException.class,
                () -> button.addAction("Acção 6", null, () -> {}));
    }
}

package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * A guia de transferência entre armazéns existia e estava completa, mas vivia no Stock — e quem
 * precisa dela vai procurá-la ao pé das Guias de Remessa, no Comercial. O atalho fecha essa
 * distância sem mudar o documento de sítio: a transferência move mercadoria entre armazéns da
 * mesma empresa, pelo que pertence mesmo ao Stock.
 */
class DeliveryGuidesShortcutTest {

    @Test
    void oAtalhoParaAsTransferenciasApareceEChamaANavegacao() {
        AtomicBoolean navegou = new AtomicBoolean(false);
        DeliveryGuidesPanel panel = new DeliveryGuidesPanel(
                mock(ComercialApiClient.class), () -> {}, () -> navegou.set(true));

        AbstractButton shortcut = findButton(panel, "Transferências entre Armazéns");
        assertNotNull(shortcut, "o atalho tem de estar visível no cabeçalho das Guias de Remessa");

        // O tooltip diz a diferença entre os dois documentos — é o que evita a escolha errada.
        String tooltip = ((JComponent) shortcut).getToolTipText();
        assertNotNull(tooltip);
        assertTrue(tooltip.contains("cliente"), tooltip);
        assertTrue(tooltip.contains("Transferência"), tooltip);

        shortcut.doClick();
        assertTrue(navegou.get(), "carregar no atalho tem de levar ao separador das transferências");
    }

    @Test
    void semAtalhoLigadoOPainelContinuaAFuncionar() {
        // O construtor antigo (dois argumentos) continua a servir quem não quer o atalho.
        DeliveryGuidesPanel panel = new DeliveryGuidesPanel(mock(ComercialApiClient.class), () -> {});

        assertNull(findButton(panel, "Transferências entre Armazéns"));
    }

    private static AbstractButton findButton(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof AbstractButton button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof Container container) {
                AbstractButton found = findButton(container, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}

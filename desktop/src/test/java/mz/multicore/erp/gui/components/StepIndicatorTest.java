package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** The step bar of the company wizard (docs/EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md CP-40). */
class StepIndicatorTest {

    @Test
    void cp40_constroiDesenhaEDescreveOPassoActual() {
        StepIndicator indicator = new StepIndicator(List.of("Identificação", "Morada", "Dados legais", "Administrador"));
        indicator.setSize(640, 58);
        indicator.setCurrent(2);

        BufferedImage image = new BufferedImage(640, 58, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            indicator.paint(g);
        } finally {
            g.dispose();
        }

        assertNotNull(indicator.getAccessibleContext());
        assertEquals("Passo 3 de 4: Dados legais", indicator.getAccessibleContext().getAccessibleDescription());
        assertEquals(2, indicator.current());
    }

    @Test
    void cp41_soNavegaParaPassosPermitidos() {
        StepIndicator indicator = new StepIndicator(List.of("A", "B", "C"));
        indicator.setSize(300, 58);
        AtomicInteger chosen = new AtomicInteger(-1);
        indicator.setNavigable(index -> index <= 1, chosen::set);

        click(indicator, 250); // passo C, não permitido
        assertEquals(-1, chosen.get());
        click(indicator, 150); // passo B
        assertEquals(1, chosen.get());
    }

    private static void click(StepIndicator indicator, int x) {
        for (var listener : indicator.getMouseListeners()) {
            listener.mouseClicked(new java.awt.event.MouseEvent(indicator, java.awt.event.MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, x, 20, 1, false));
        }
    }
}

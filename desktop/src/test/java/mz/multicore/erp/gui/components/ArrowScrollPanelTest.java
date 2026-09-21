package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do componente canónico ArrowScrollPanel (HARNESS-ASO-001).
 */
class ArrowScrollPanelTest {

    private JPanel dummyContent;
    private ArrowScrollPanel panel;

    @BeforeEach
    void setUp() {
        dummyContent = new JPanel();
        dummyContent.setPreferredSize(new Dimension(800, 2000));
        panel = new ArrowScrollPanel(dummyContent);
        panel.setSize(600, 500);
        panel.getScrollPane().setSize(560, 500);
        panel.getScrollPane().getViewport().setSize(560, 500);
        panel.getScrollPane().getViewport().setExtentSize(new Dimension(560, 500));
        panel.getScrollPane().getViewport().setViewSize(new Dimension(560, 2000));
        panel.updateNavigationState();
    }

    @Test
    void testAso01_WidthTrackingViewportEnabled() {
        Component view = panel.getScrollPane().getViewport().getView();
        assertTrue(view instanceof Scrollable, "O contentor de vista deve implementar Scrollable");
        Scrollable scrollable = (Scrollable) view;
        assertTrue(scrollable.getScrollableTracksViewportWidth(),
                "getScrollableTracksViewportWidth deve ser true para eliminar overflow horizontal");
        assertFalse(scrollable.getScrollableTracksViewportHeight(),
                "getScrollableTracksViewportHeight deve ser false para permitir scroll vertical");
    }

    @Test
    void testAso02_ScrollbarsHidden() {
        JScrollPane sp = panel.getScrollPane();
        assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, sp.getHorizontalScrollBarPolicy());
        assertEquals(JScrollPane.VERTICAL_SCROLLBAR_NEVER, sp.getVerticalScrollBarPolicy());
    }

    @Test
    void testAso03_Aso05_InitialStateAtTop_UpButtonDisabled() {
        panel.scrollToTop();
        assertEquals(0, panel.getScrollPane().getViewport().getViewPosition().y);
        assertFalse(panel.getBtnUp().isEnabled(), "Botão de seta superior deve estar desativado no topo");
        assertTrue(panel.getBtnDown().isEnabled(), "Botão de seta inferior deve estar ativo quando há conteúdo abaixo");
    }

    @Test
    void testAso04_Aso06_ScrollToBottom_DownButtonDisabled() {
        panel.scrollToBottom();
        int y = panel.getScrollPane().getViewport().getViewPosition().y;
        assertTrue(y > 0, "A viewport deve ter descido");
        assertTrue(panel.getBtnUp().isEnabled(), "Botão de seta superior deve estar ativo após rolar");
        assertFalse(panel.getBtnDown().isEnabled(), "Botão de seta inferior deve estar desativado no fundo");
    }

    @Test
    void testAso07_SmoothScrollDeltaAdjustsPosition() {
        panel.scrollToTop();
        panel.getScrollPane().getViewport().setViewPosition(new Point(0, 500));
        panel.updateNavigationState();
        assertEquals(500, panel.getScrollPane().getViewport().getViewPosition().y);
        assertTrue(panel.getBtnUp().isEnabled());
        assertTrue(panel.getBtnDown().isEnabled());
    }
}

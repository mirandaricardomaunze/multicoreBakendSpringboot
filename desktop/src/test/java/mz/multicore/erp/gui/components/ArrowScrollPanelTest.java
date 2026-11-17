package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the canonical ArrowScrollPanel (HARNESS-ASO-001, revised 2026-11: slim scrollbar instead of arrows).
 */
class ArrowScrollPanelTest {

    private ArrowScrollPanel panel;

    @BeforeEach
    void setUp() {
        JPanel dummyContent = new JPanel();
        dummyContent.setPreferredSize(new Dimension(800, 2000));
        panel = new ArrowScrollPanel(dummyContent);
        panel.setSize(600, 500);
        panel.getScrollPane().setSize(600, 500);
        panel.getScrollPane().getViewport().setSize(594, 500);
        panel.getScrollPane().getViewport().setExtentSize(new Dimension(594, 500));
        panel.getScrollPane().getViewport().setViewSize(new Dimension(594, 2000));
    }

    @Test
    void aso01_widthTrackingViewportEnabled() {
        Component view = panel.getScrollPane().getViewport().getView();
        assertTrue(view instanceof Scrollable, "O contentor de vista deve implementar Scrollable");
        Scrollable scrollable = (Scrollable) view;
        assertTrue(scrollable.getScrollableTracksViewportWidth(), "Sem overflow horizontal");
        assertFalse(scrollable.getScrollableTracksViewportHeight(), "Cresce na vertical");
    }

    @Test
    void aso02_slimVerticalScrollbarOnlyWhenNeeded() {
        JScrollPane sp = panel.getScrollPane();
        assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, sp.getHorizontalScrollBarPolicy());
        assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, sp.getVerticalScrollBarPolicy());
        assertInstanceOf(SlimScrollBarUI.class, sp.getVerticalScrollBar().getUI());
    }

    @Test
    void aso03_noArrowRail() {
        assertTrue(buttons(panel).isEmpty(), "A calha de setas saiu: o painel não tem botões próprios");
    }

    @Test
    void aso04_scrollToBottomAndTop() {
        panel.scrollToBottom();
        assertTrue(panel.getScrollPane().getViewport().getViewPosition().y > 0, "A viewport deve ter descido");
        panel.scrollToTop();
        assertEquals(0, panel.getScrollPane().getViewport().getViewPosition().y);
    }

    @Test
    void aso05_maxScrollFollowsContentHeight() {
        assertEquals(1500, panel.getMaxScrollY());
    }

    private static List<AbstractButton> buttons(Container container) {
        List<AbstractButton> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JScrollBar) continue; // the scrollbar's own UI buttons are not a rail
            if (child instanceof AbstractButton b) found.add(b);
            if (child instanceof Container c) found.addAll(buttons(c));
        }
        return found;
    }
}

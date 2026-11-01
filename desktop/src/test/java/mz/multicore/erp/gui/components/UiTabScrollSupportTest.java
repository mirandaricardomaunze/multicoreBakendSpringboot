package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/COMERCIAL_PANEL_DECOMPOSITION_SPEC.md. */
class UiTabScrollSupportTest {

    /** Janela de 1366 px menos a sidebar recolhida e as margens. */
    private static final int AVAILABLE_AT_1366 = 1270;
    private static final List<String> COMMERCIAL = List.of("Cotações", "Faturação", "Pedidos & Separação", "Recibos",
            "Guias", "Notas de Crédito", "Notas de Débito", "Contas Correntes", "Promoções", "Movimentos");

    @Test
    @DisplayName("As dez abas de Vendas cabem num ecrã de 1366 px, medidas pelo componente real")
    void commercialTabsFit() throws Exception {
        JTabbedPane tabs = tabs(COMMERCIAL, AVAILABLE_AT_1366);

        int total = 0;
        for (int i = 0; i < tabs.getTabCount(); i++) total += tabs.getBoundsAt(i).width;

        assertThat(total).isLessThanOrEqualTo(AVAILABLE_AT_1366);
    }

    @Test
    @DisplayName("As setas não contam como abas e têm tamanho de clique")
    void buttonsAreNotTabs() throws Exception {
        JTabbedPane tabs = tabs(COMMERCIAL, 400);

        assertThat(tabs.getTabCount()).isEqualTo(COMMERCIAL.size());
        assertThat(UiTabScrollSupport.button(SwingUtilities.EAST).getPreferredSize())
                .isEqualTo(new Dimension(30, 30));
    }

    @Test
    @DisplayName("A roda do rato sobre as abas desliza a barra")
    void wheelOverStripScrolls() throws Exception {
        JTabbedPane tabs = tabs(COMMERCIAL, 400);
        JViewport viewport = viewport(tabs);
        assertThat(viewport.getViewPosition()).isEqualTo(new Point(0, 0));

        SwingUtilities.invokeAndWait(() -> tabs.dispatchEvent(wheel(tabs, 5)));

        assertThat(viewport.getViewPosition().x).isPositive();
    }

    @Test
    @DisplayName("A roda do rato sobre o conteúdo não mexe nas abas")
    void wheelOverContentIgnored() throws Exception {
        JTabbedPane tabs = tabs(COMMERCIAL, 400);
        JViewport viewport = viewport(tabs);

        SwingUtilities.invokeAndWait(() -> tabs.dispatchEvent(wheel(tabs, 250)));

        assertThat(viewport.getViewPosition()).isEqualTo(new Point(0, 0));
    }

    private static MouseWheelEvent wheel(JTabbedPane tabs, int y) {
        return new MouseWheelEvent(tabs, MouseWheelEvent.MOUSE_WHEEL, 0, 0, 100, y, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1);
    }

    private static JViewport viewport(JTabbedPane tabs) {
        for (Component child : tabs.getComponents()) {
            if (child instanceof JViewport viewport) return viewport;
        }
        throw new AssertionError("Sem barra com scroll");
    }

    private static JTabbedPane tabs(List<String> titles, int width) throws Exception {
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            UIHelper.styleTabbedPaneMulticore(tabs);
            titles.forEach(title -> tabs.addTab(title, UIHelper.icon("fas-tags", 16, UIHelper.ACCENT_BLUE), new JPanel().add(new JLabel(title))));
            tabs.setSize(width, 300);
            tabs.doLayout();
            holder[0] = tabs;
        });
        return holder[0];
    }
}

package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Geometria da barra de separadores (docs/SEPARADORES_SPEC.md, TAB-09 e TAB-10). */
class TabStripGeometryHarnessTest {

    private static final String[] HR = {"Visão Geral", "Colaboradores", "Contratos", "Recibos de Salário", "Faltas",
            "Ponto", "Férias", "Descontos", "Retenções", "Cessações", "Despesas"};

    @Test
    @DisplayName("TAB-09/TAB-10: \"Mais\" inteiro na barra e o primeiro separador visível nunca cortado")
    void moreIsFullHeightAndFirstVisibleTabIsWhole() throws Exception {
        JFrame[] frame = new JFrame[1];
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            UIHelper.styleTabbedPaneMulticore(tabs);
            for (String title : HR) tabs.addTab(title, UIHelper.icon("fas-users", 16, UIHelper.ACCENT), new JPanel());
            frame[0] = new JFrame();
            frame[0].setUndecorated(true);
            frame[0].add(tabs);
            frame[0].setSize(1110, 200);
            frame[0].setVisible(true);
            tabs.setSelectedIndex(9);
            holder[0] = tabs;
        });
        for (int i = 0; i < 3; i++) SwingUtilities.invokeAndWait(() -> {});
        try {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = holder[0];
                List<JButton> more = new ArrayList<>();
                JViewport viewport = null;
                for (Component child : tabs.getComponents()) {
                    if (child instanceof JButton button) more.add(button);
                    if (child instanceof JViewport v) viewport = v;
                }
                assertThat(more).hasSize(2);
                for (JButton button : more) {
                    Rectangle b = button.getBounds();
                    assertThat(b.height).as("\"Mais\" com altura inteira (era 2 px)").isEqualTo(30);
                    assertThat(b.y + b.height).as("dentro da barra").isLessThanOrEqualTo(viewport.getHeight());
                }
                int originX = viewport.getX();
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Rectangle tab = tabs.getBoundsAt(i);
                    boolean cutOnTheLeft = tab.x < originX && tab.x + tab.width > originX;
                    assertThat(cutOnTheLeft).as("separador cortado à esquerda: " + tabs.getTitleAt(i)).isFalse();
                }
                assertThat(tabs.getBoundsAt(9).x + tabs.getBoundsAt(9).width)
                        .as("o separador escolhido continua visível").isLessThanOrEqualTo(viewport.getX() + viewport.getWidth());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> frame[0].dispose());
        }
    }
}

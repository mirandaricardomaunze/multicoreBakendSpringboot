package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.UIResource;
import java.awt.Component;
import java.awt.Graphics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness dos separadores (docs/SEPARADORES_SPEC.md): "Mais ▾", aspecto sóbrio e memória do separador. */
class TabStripHarnessTest {

    private static final List<String> COMMERCIAL = List.of("Cotações", "Faturação", "Pedidos & Separação", "Recibos",
            "Guias", "Notas de Crédito", "Notas de Débito", "Contas Correntes", "Promoções", "Movimentos");

    @Test
    @DisplayName("TAB-01: separadores que não cabem aparecem em \"Mais\" e escolhê-los selecciona-os")
    void tab01_overflowListsHiddenTabs() throws Exception {
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = tabs(COMMERCIAL, 500));
        JTabbedPane tabs = holder[0];

        List<JButton> more = moreButtons(tabs);
        assertThat(more).as("duas metades do controlo \"Mais ▾\", sem setas").hasSize(2);
        assertThat(more).allMatch(Component::isVisible);
        assertThat(more.get(0).getToolTipText()).isEqualTo("Ver os separadores que não cabem");

        List<Integer> hidden = UiTabOverflow.hiddenTabs(tabs);
        assertThat(hidden).isNotEmpty().contains(COMMERCIAL.size() - 1);
        JPopupMenu menu = UiTabOverflow.menu(tabs);
        assertThat(menu.getComponentCount()).isEqualTo(hidden.size());

        JMenuItem last = (JMenuItem) menu.getComponent(menu.getComponentCount() - 1);
        assertThat(last.getText()).isEqualTo("Movimentos");
        last.doClick();
        assertThat(tabs.getSelectedIndex()).isEqualTo(COMMERCIAL.size() - 1);
    }

    @Test
    @DisplayName("TAB-02: quando tudo cabe não há \"Mais\" nem separadores escondidos")
    void tab02_noOverflowWhenEverythingFits() throws Exception {
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = tabs(List.of("Visão Geral", "Contratos", "Ponto"), 1200));
        assertThat(moreButtons(holder[0])).noneMatch(Component::isVisible);
        assertThat(UiTabOverflow.hiddenTabs(holder[0])).isEmpty();
    }

    @Test
    @DisplayName("TAB-03: \"Mais\" nunca fica desactivado no fim da barra")
    void tab03_moreStaysEnabled() throws Exception {
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = tabs(COMMERCIAL, 500));
        JButton button = moreButtons(holder[0]).get(1);
        button.setEnabled(false);
        assertThat(button.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("TAB-04: ícones numa cor sóbria; o activo usa a cor de destaque")
    void tab04_quietIcons() {
        Icon colourful = UIHelper.icon("fas-truck", 16, UIHelper.ACCENT_PINK);
        FontIcon muted = (FontIcon) UiTabIcons.tinted(colourful, UIHelper.TEXT_MUTED);
        FontIcon active = (FontIcon) UiTabIcons.tinted(colourful, UIHelper.ACCENT);

        assertThat(muted.getIconColor()).isEqualTo(UIHelper.TEXT_MUTED);
        assertThat(active.getIconColor()).isEqualTo(UIHelper.ACCENT);
        assertThat(muted.getIconSize()).isEqualTo(16);
        assertThat(((FontIcon) colourful).getIconColor()).as("o ícone original não muda").isEqualTo(UIHelper.ACCENT_PINK);

        Icon plain = new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) { }
            @Override public int getIconWidth() { return 8; }
            @Override public int getIconHeight() { return 8; }
        };
        assertThat(UiTabIcons.tinted(plain, UIHelper.TEXT_MUTED)).isSameAs(plain);
    }

    @Test
    @DisplayName("TAB-05: a memória reconhece a barra pelos nomes, ignorando contadores")
    void tab05_memoryKeyAndLookup() {
        JTabbedPane a = new JTabbedPane();
        for (String t : List.of("Níveis de Stock", "Alertas (3)", "Transferências entre Armazéns")) a.addTab(t, new JPanel());
        JTabbedPane b = new JTabbedPane();
        for (String t : List.of("Níveis de Stock", "Alertas (12)", "Transferências entre Armazéns")) b.addTab(t, new JPanel());

        assertThat(UiTabMemory.key(a)).isEqualTo(UiTabMemory.key(b));
        assertThat(UiTabMemory.indexOf(b, "Transferências entre Armazéns")).isEqualTo(2);
        assertThat(UiTabMemory.indexOf(b, "Alertas (3)")).isEqualTo(1);
        assertThat(UiTabMemory.indexOf(b, "Separador que já não existe")).isEqualTo(-1);
        b.setEnabledAt(2, false);
        assertThat(UiTabMemory.indexOf(b, "Transferências entre Armazéns")).as("separador desactivado não é reaberto").isEqualTo(-1);
    }

    @Test
    @DisplayName("TAB-06: regras ligadas no estilo único dos separadores")
    void tab06_wiredInTheSingleStyle() throws IOException {
        String styler = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/components/UiTabbedPaneStyler.java"));
        String multicore = styler.substring(styler.indexOf("static void styleTabbedPaneMulticore"));
        assertThat(multicore)
                .contains("UiTabOverflow.button(tabPane, direction)")
                .contains("UiTabIcons.tinted(icon, isSelected ? UIHelper.ACCENT : UIHelper.TEXT_MUTED)")
                .contains("font.deriveFont(Font.PLAIN)")
                .contains("UiTabMemory.install(tabbedPane)");
        String memory = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/components/UiTabMemory.java"));
        assertThat(memory).as("diálogos abrem sempre no primeiro separador").contains("instanceof JFrame");
        assertThat(memory).as("separador escolhido pelo programa ganha").contains("state.chosenByProgram");
    }

    private static JTabbedPane tabs(List<String> titles, int width) {
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        for (String title : titles) tabs.addTab(title, UIHelper.icon("fas-file", 16, UIHelper.ACCENT_BLUE), new JPanel());
        tabs.setSize(width, 300);
        tabs.doLayout();
        return tabs;
    }

    private static List<JButton> moreButtons(JTabbedPane tabs) {
        List<JButton> found = new ArrayList<>();
        for (Component child : tabs.getComponents()) {
            if (child instanceof JButton button && child instanceof UIResource
                    && "Mais separadores".equals(button.getAccessibleContext().getAccessibleName())) {
                found.add(button);
            }
        }
        return found;
    }
}

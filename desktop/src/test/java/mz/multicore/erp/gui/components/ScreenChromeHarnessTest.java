package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness do ecrã mais limpo (docs/ECRA_CAMINHO_E_MENU_SPEC.md): caminho na topbar, cabeçalho numa fila, menu lateral. */
class ScreenChromeHarnessTest {

    private static final Path COMPONENTS = Path.of("src/main/java/mz/multicore/erp/gui/components");

    @Test
    @DisplayName("ECR-01: cabeçalho da tabela numa só fila (título, filtros, acções)")
    void ecr01_oneRowHeader() {
        JPanel filters = new JPanel();
        filters.setPreferredSize(new java.awt.Dimension(400, 30));
        JPanel top = UIHelper.tableCardTop("Clientes", filters, new JButton("Actualizar"));
        BorderLayout layout = (BorderLayout) top.getLayout();

        assertThat(layout.getLayoutComponent(BorderLayout.WEST)).isInstanceOf(JLabel.class);
        assertThat(layout.getLayoutComponent(BorderLayout.CENTER)).isSameAs(filters);
        assertThat(layout.getLayoutComponent(BorderLayout.EAST)).isNotNull();
        assertThat(top.getPreferredSize().height).as("uma fila, não duas").isLessThan(30 + 34);
    }

    @Test
    @DisplayName("ECR-02: o título sai quando o separador já tem o mesmo nome")
    void ecr02_titleHiddenWhenTabRepeatsIt() {
        JLabel repeated = headingOf(inTab("Transferências entre Armazéns", "Transferências entre Armazéns"));
        JLabel counted = headingOf(inTab("Alertas (3)", "Alertas"));
        JLabel prefix = headingOf(inTab("Movimentos & Rastreabilidade", "Movimentos"));
        JLabel different = headingOf(inTab("Clientes", "Clientes Bloqueados"));

        assertThat(repeated.isVisible()).isFalse();
        assertThat(counted.isVisible()).as("contador entre parênteses não conta").isFalse();
        assertThat(prefix.isVisible()).as("o separador já diz mais do que o título").isFalse();
        assertThat(different.isVisible()).as("título com informação a mais fica").isTrue();
    }

    @Test
    @DisplayName("ECR-03: sem separador, o título continua visível")
    void ecr03_titleStaysWithoutTabs() {
        JPanel screen = new JPanel(new BorderLayout());
        JPanel top = UIHelper.tableCardTop("Pedidos dos Clientes", new JPanel());
        screen.add(top, BorderLayout.NORTH);
        assertThat(headingOf(top).isVisible()).isTrue();
    }

    @Test
    @DisplayName("ECR-04: a topbar mostra o caminho Módulo › Separador e limpa-o ao mudar de módulo")
    void ecr04_topBarPath() {
        TopNavBar bar = new TopNavBar("Stock", "");
        bar.setSection("Transferências entre Armazéns");
        assertThat(bar.getSection()).isEqualTo("Transferências entre Armazéns");

        bar.setTitle("Stock");
        assertThat(bar.getSection()).as("o mesmo módulo mantém o separador").isEqualTo("Transferências entre Armazéns");

        bar.setTitle("Compras");
        assertThat(bar.getSection()).as("outro módulo começa sem separador").isNull();
    }

    @Test
    @DisplayName("ECR-05: menu recolhido em ecrã pequeno; a escolha do utilizador ganha sempre")
    void ecr05_sidebarStartState() {
        assertThat(SidebarCollapsePreference.decide(null, 1366)).isTrue();
        assertThat(SidebarCollapsePreference.decide(null, 1536)).isFalse();
        assertThat(SidebarCollapsePreference.decide(null, 1920)).isFalse();
        assertThat(SidebarCollapsePreference.decide("false", 1366)).isFalse();
        assertThat(SidebarCollapsePreference.decide("true", 1920)).isTrue();
        assertThat(SidebarCollapsePreference.decide(null, 0)).as("sem ecrã (headless) abre como hoje").isFalse();
    }

    @Test
    @DisplayName("ECR-06: só o clique do utilizador é lembrado; itens tardios seguem o estado do menu")
    void ecr06_onlyUserTogglesAreRemembered() throws IOException {
        String sidebar = Files.readString(COMPONENTS.resolve("CollapsibleSidebar.java"));
        assertThat(sidebar).contains("public void toggle() { setCollapsedState(!collapsed); SidebarCollapsePreference.remember(collapsed); }");
        assertThat(sidebar).contains("item.setCollapsed(collapsed);").contains("section.setCollapsed(collapsed);");

        String mainFrame = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/MainFrame.java"));
        assertThat(mainFrame).as("o POS recolhe o menu sem gravar preferência").doesNotContain("SidebarCollapsePreference.remember");
        assertThat(mainFrame).contains("breadcrumb.followVisibleCard()");

        String styler = Files.readString(COMPONENTS.resolve("UiFormComponentStyler.java"));
        assertThat(styler).contains("return TableCardTop.build(title, filters, actions);");
    }

    private static JPanel inTab(String tabTitle, String tableTitle) {
        JPanel page = new JPanel(new BorderLayout());
        JPanel card = new JPanel(new BorderLayout());
        page.add(card, BorderLayout.CENTER);
        JPanel top = UIHelper.tableCardTop(tableTitle, new JPanel(), new JButton("Actualizar"));
        card.add(top, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Outro", new JPanel());
        tabs.addTab(tabTitle, page);
        return top;
    }

    private static JLabel headingOf(JPanel top) {
        BorderLayout layout = (BorderLayout) top.getLayout();
        Object west = layout.getLayoutComponent(BorderLayout.WEST);
        return (JLabel) (west != null ? west : layout.getLayoutComponent(BorderLayout.CENTER));
    }
}

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
    @DisplayName("ECR-02: o título sai quando repete o separador; fica quando diz outra coisa ou distingue tabelas")
    void ecr02_titleHiddenWhenTabRepeatsIt() {
        assertThat(headingOf(inTab("Transferências entre Armazéns", "Transferências entre Armazéns")).isVisible()).isFalse();
        assertThat(headingOf(inTab("Alertas (3)", "Alertas")).isVisible()).as("contador não conta").isFalse();
        assertThat(headingOf(inTab("Movimentos & Rastreabilidade", "Movimentos")).isVisible()).isFalse();
        assertThat(headingOf(inTab("Recibos", "Recibos Emitidos (Liquidações)")).isVisible()).isFalse();
        assertThat(headingOf(inTab("Pedidos & Separação", "Central de Pedidos e Separação")).isVisible()).as("& lê-se e").isFalse();
        assertThat(headingOf(inTab("Faturação", "Faturas Recentes")).isVisible()).as("diz outra coisa").isTrue();
        assertThat(headingOf(inTab("Guias", "Guiaslote")).isVisible()).as("só palavras inteiras").isTrue();

        JPanel page = new JPanel(new java.awt.GridLayout(2, 1));
        JPanel first = UIHelper.tableCardTop("Clientes Activos", new JPanel());
        JPanel second = UIHelper.tableCardTop("Clientes Bloqueados", new JPanel());
        page.add(first);
        page.add(second);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Clientes", page);
        java.awt.event.HierarchyEvent shown = new java.awt.event.HierarchyEvent(first, java.awt.event.HierarchyEvent.HIERARCHY_CHANGED,
                first, page, java.awt.event.HierarchyEvent.SHOWING_CHANGED);
        for (var l : first.getHierarchyListeners()) l.hierarchyChanged(shown);
        assertThat(headingOf(first).isVisible()).as("duas tabelas no mesmo separador: os títulos distinguem-nas").isTrue();
    }

    @Test
    @DisplayName("ECR-08: filtros nunca esmagados; numa fila quando cabem, senão em segunda fila com a largura toda")
    void ecr08_filtersNeverSqueezed() {
        JPanel filters = new JPanel();
        filters.setPreferredSize(new java.awt.Dimension(600, 38));
        JButton action = new JButton("Nova Cotação");
        action.setPreferredSize(new java.awt.Dimension(150, 38));
        JPanel top = UIHelper.tableCardTop("Central de Pedidos", filters, action);

        top.setSize(1300, 60);
        top.doLayout();
        assertThat(filters.getY()).as("uma fila").isLessThan(10);
        assertThat(filters.getWidth()).isGreaterThanOrEqualTo(600);
        java.awt.Component actions = ((BorderLayout) top.getLayout()).getLayoutComponent(BorderLayout.EAST);
        assertThat(filters.getY() + filters.getHeight() / 2).as("filtros e botões centrados na mesma linha")
                .isEqualTo(actions.getY() + actions.getHeight() / 2);

        top.setSize(700, 100);
        top.doLayout();
        assertThat(filters.getY()).as("segunda fila").isGreaterThan(20);
        assertThat(filters.getWidth()).as("filtros com a largura toda").isEqualTo(700 - top.getInsets().left - top.getInsets().right);
        assertThat(top.getPreferredSize().height).as("altura pedida inclui as duas filas").isGreaterThan(38 + 30);
    }

    @Test
    @DisplayName("ECR-09: todos os ecrãs com filtros usam o cabeçalho comum")
    void ecr09_everyScreenUsesTheSharedHeader() throws IOException {
        Path gui = Path.of("src/main/java/mz/multicore/erp/gui");
        java.util.Set<String> helpers = java.util.Set.of("PhysicalInventoryViewCoordinator.java");
        try (var files = Files.walk(gui)) {
            java.util.List<String> offenders = files
                    .filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.toString().replace('\\', '/').contains("/components/"))
                    .filter(f -> !f.getFileName().toString().endsWith("Dialog.java"))
                    .filter(f -> !helpers.contains(f.getFileName().toString()))
                    .filter(f -> {
                        String src = read(f);
                        boolean hasFilters = src.contains("TableFilter.bar(") || src.contains("UIHelper.filterBar(")
                                || src.contains("TableFilter.toolbar(") || src.contains("TableFilter.searchField(");
                        return hasFilters && !src.contains("UIHelper.tableCardTop(") && !src.contains("ResponsiveSupport.cardTop(");
                    })
                    .map(f -> f.getFileName().toString())
                    .toList();
            assertThat(offenders).as("ecrãs com cabeçalho feito à mão").isEmpty();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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

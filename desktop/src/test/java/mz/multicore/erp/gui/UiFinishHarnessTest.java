package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.SimplePieChart;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/UI_ACABAMENTO_SPEC.md. */
class UiFinishHarnessTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Pattern MENU = Pattern.compile("createActionMenuButton\\(\"([^\"]+)\"\\)");
    private static final Set<String> VAGUE = Set.of("Operações", "Decisão", "Ações", "Acções", "Opções");

    @Test
    @DisplayName("UA-01: Nenhum menu tem nome genérico")
    void ua01_noVagueMenus() throws IOException {
        assertThat(menuNames()).isNotEmpty().doesNotContainAnyElementsOf(VAGUE);
    }

    @Test
    @DisplayName("UA-02: \"Mais acções\" tem uma só grafia")
    void ua02_oneSpelling() throws IOException {
        assertThat(menuNames().stream().filter(name -> name.toLowerCase().startsWith("mais a")))
                .isNotEmpty().containsOnly("Mais acções");
    }

    @Test
    @DisplayName("UA-03: Gráfico de barras vazio mostra a dica")
    void ua03_barChartHint() {
        SimpleBarChart chart = new SimpleBarChart("Vendas");
        chart.setEmptyHint("Registe uma venda.");

        assertThat(chart.getEmptyHint()).isEqualTo("Registe uma venda.");
        paint(chart);
    }

    @Test
    @DisplayName("UA-03: Gráfico circular vazio mostra a dica")
    void ua03_pieChartHint() {
        SimplePieChart chart = new SimplePieChart("Canais", false);
        chart.setEmptyHint("Registe uma venda.");

        assertThat(chart.getEmptyHint()).isEqualTo("Registe uma venda.");
        paint(chart);
    }

    @Test
    @DisplayName("UA-04: Sem dica, o gráfico mostra só a mensagem")
    void ua04_noHintByDefault() {
        SimpleBarChart chart = new SimpleBarChart("Salários");

        assertThat(chart.getEmptyHint()).isNull();
        paint(chart);
    }

    @Test
    @DisplayName("UA-05: O Painel Inicial sugere o passo seguinte")
    void ua05_dashboardHints() throws IOException {
        String source = Files.readString(SOURCES.resolve("mz/multicore/erp/gui/DashboardOverviewTab.java"));

        assertThat(source).contains("financialChart.setEmptyHint(", "salesChannelPieChart.setEmptyHint(",
                "financialStructurePieChart.setEmptyHint(");
    }

    @Test
    @DisplayName("UA-06, 07 e 08: A coluna de selecção acompanha as linhas")
    void ua06_gutterFollowsRows() throws Exception {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"A"}, 0);
        JScrollPane[] holder = new JScrollPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(model);
            UIHelper.styleTable(table);
            holder[0] = new JScrollPane(table);
        });
        JScrollPane scroll = holder[0];
        assertThat(scroll.getRowHeader()).as("coluna instalada").isNotNull();
        assertThat(scroll.getRowHeader().isVisible()).isFalse();
        assertThat(scroll.getCorner(JScrollPane.UPPER_LEFT_CORNER).isVisible()).isFalse();

        SwingUtilities.invokeAndWait(() -> model.addRow(new Object[]{"x"}));
        assertThat(scroll.getRowHeader().isVisible()).isTrue();
        assertThat(scroll.getCorner(JScrollPane.UPPER_LEFT_CORNER).isVisible()).isTrue();

        SwingUtilities.invokeAndWait(() -> model.removeRow(0));
        assertThat(scroll.getRowHeader().isVisible()).isFalse();
    }

    private static void paint(java.awt.Component chart) {
        chart.setSize(420, 220);
        BufferedImage image = new BufferedImage(420, 220, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        chart.paint(graphics);
        graphics.dispose();
    }

    private static List<String> menuNames() throws IOException {
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.endsWith("UIHelper.java")) continue;
                Matcher matcher = MENU.matcher(Files.readString(file));
                while (matcher.find()) names.add(matcher.group(1));
            }
        }
        return names;
    }
}

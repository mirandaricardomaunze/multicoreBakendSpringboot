package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness da faixa compacta de KPIs (docs/KPI_FAIXA_COMPACTA_SPEC.md). */
class KpiStripHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");

    /** Ecrãs em que o indicador é o próprio conteúdo: só estes mantêm os cartões altos. */
    private static final Set<String> HERO_SCREENS = Set.of(
            "DashboardOverviewTab.java", "DashboardOperationsTab.java", "SellerDashboardViewCoordinator.java",
            "StockProductDetailHeroBuilder.java", "StrategicPulseWidget.java", "OperationalPulseWidget.java",
            "ProfitAnalyticsWidget.java");

    @Test
    @DisplayName("KFC-01: createGrid devolve a faixa compacta e encolhe cada cartão adicionado")
    void kfc01_gridCompactsCards() {
        JPanel strip = KpiCard.createGrid(4);
        assertThat(strip.getLayout()).isInstanceOf(GridLayout.class);

        JLabel value = new JLabel("38");
        ModernPanel card = KpiCard.createCard("Pedidos Pendentes", value, "Aguardam decisão", "fas-hourglass-half", UIHelper.PENDING_YELLOW);
        assertThat(card.getPreferredSize().height).isEqualTo(KpiCard.STANDARD_CARD_HEIGHT);

        strip.add(card);

        assertThat(card.getPreferredSize().height).isEqualTo(KpiCompactLayout.HEIGHT);
        assertThat(card.getMinimumSize().height).isEqualTo(KpiCompactLayout.HEIGHT);
        assertThat(KpiCompactLayout.HEIGHT).isLessThanOrEqualTo(KpiCard.STANDARD_CARD_HEIGHT * 6 / 10);
        assertThat(KpiCompactLayout.isCompact(card)).isTrue();
    }

    @Test
    @DisplayName("KFC-02: o ecrã continua a actualizar o mesmo JLabel do valor e da nota")
    void kfc02_labelsStayLive() {
        JLabel value = new JLabel("0");
        JLabel note = new JLabel("Sem dados");
        ModernPanel card = KpiCard.createCard("Valor em Análise", value, null, note, "fas-coins", UIHelper.ACCENT_BLUE);
        KpiCard.createGrid(4).add(card);

        value.setText("1.250,00 MT");
        note.setText("3 pedidos");

        assertThat(contains(card, value)).isTrue();
        assertThat(contains(card, note)).isTrue();
        assertThat(value.getParent()).as("nota na mesma linha do valor").isSameAs(note.getParent());
        assertThat(value.getFont().getSize()).isLessThanOrEqualTo(17);
    }

    @Test
    @DisplayName("KFC-03: createHeroGrid mantém os cartões altos dos painéis")
    void kfc03_heroKeepsFullHeight() {
        JPanel hero = KpiCard.createHeroGrid(4, 10, 10);
        ModernPanel card = KpiCard.createCard("Vendas", "15.000 MT", "Hoje", "fas-chart-line", UIHelper.ACCENT_BLUE);
        hero.add(card);
        assertThat(card.getPreferredSize().height).isEqualTo(KpiCard.STANDARD_CARD_HEIGHT);
        assertThat(KpiCompactLayout.isCompact(card)).isFalse();
    }

    @Test
    @DisplayName("KFC-04: um cartão compacto continua clicável para filtrar")
    void kfc04_compactCardKeepsDrilldown() {
        AtomicBoolean clicked = new AtomicBoolean();
        ModernPanel card = KpiCard.createInteractiveCard("Rupturas", new JLabel("5"), "Itens esgotados",
                "fas-boxes", UIHelper.REJECTED_RED, "Filtrar rupturas", () -> clicked.set(true));
        KpiCard.createGrid(4).add(card);

        assertThat(card.getCursor().getType()).isEqualTo(Cursor.HAND_CURSOR);
        MouseEvent click = new MouseEvent(card, MouseEvent.MOUSE_CLICKED, 0, 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        for (var listener : card.getMouseListeners()) listener.mouseClicked(click);
        assertThat(clicked).isTrue();
    }

    @Test
    @DisplayName("KFC-05: só os painéis de indicadores usam createHeroGrid")
    void kfc05_heroGridOnlyOnDashboards() throws IOException {
        try (Stream<Path> files = Files.walk(GUI)) {
            List<String> offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("KpiCard.java"))
                    .filter(p -> read(p).contains("KpiCard.createHeroGrid("))
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !HERO_SCREENS.contains(name))
                    .toList();
            assertThat(offenders).as("ecrãs com tabela devem usar KpiCard.createGrid (faixa compacta)").isEmpty();
        }
    }

    @Test
    @DisplayName("KFC-06: os cartões KPI não são montados em grelhas improvisadas por cima de tabelas")
    void kfc06_noAdHocKpiGridsAboveTables() throws IOException {
        try (Stream<Path> files = Files.walk(GUI)) {
            List<String> offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        String src = read(p);
                        return src.contains("KpiCard.create") && src.contains("new JTable")
                                && !src.contains("KpiCard.createGrid(") && !src.contains("KpiCard.createHeroGrid(");
                    })
                    .map(p -> p.getFileName().toString())
                    .toList();
            assertThat(offenders).isEmpty();
        }
    }

    @Test
    @DisplayName("KFC-07: um valor grande nunca passa da borda do cartão; a nota cede primeiro")
    void kfc07_largeValueNeverOverflows() {
        JLabel value = new JLabel("0");
        JLabel note = new JLabel("Total valorizado em mercadoria");
        ModernPanel card = KpiCard.createCard("Valor Movimentado", value, null, note, "fas-coins", UIHelper.ACCENT_BLUE);
        JPanel strip = KpiCard.createGrid(5);
        strip.add(card);
        value.setText("1.125.450.000.000,00 MT");

        for (int width : new int[]{150, 190, 260, 400}) {
            card.setSize(width, KpiCompactLayout.HEIGHT);
            layoutTree(card);

            java.awt.Rectangle inCard = javax.swing.SwingUtilities.convertRectangle(value.getParent(), value.getBounds(), card);
            int innerRight = card.getWidth() - card.getInsets().right;
            assertThat(inCard.x + inCard.width).as("valor dentro do cartão com %d px", width).isLessThanOrEqualTo(innerRight);
            int needed = value.getFontMetrics(value.getFont()).stringWidth(value.getText());
            if (needed > value.getWidth()) {
                assertThat(value.getToolTipText()).as("valor cortado mostra o texto completo").isEqualTo(value.getText());
            }
            if (note.isVisible() && note.getWidth() > 0) {
                java.awt.Rectangle noteInCard = javax.swing.SwingUtilities.convertRectangle(note.getParent(), note.getBounds(), card);
                assertThat(noteInCard.x + noteInCard.width).isLessThanOrEqualTo(innerRight);
                assertThat(noteInCard.x).as("nota depois do valor").isGreaterThanOrEqualTo(inCard.x + inCard.width);
            }
        }
    }

    private static void layoutTree(java.awt.Container root) {
        root.doLayout();
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof java.awt.Container nested) layoutTree(nested);
        }
    }

    private static boolean contains(java.awt.Container root, java.awt.Component target) {
        for (java.awt.Component child : root.getComponents()) {
            if (child == target) return true;
            if (child instanceof java.awt.Container nested && contains(nested, target)) return true;
        }
        return false;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of the loading state of tables (docs/ESTADO_CARREGAR_SPEC.md). */
class TableLoadingStateHarnessTest {

    @Test
    @DisplayName("EC-01: tabela vazia a carregar diz \"A carregar…\", não \"Sem registos.\"")
    void ec01_emptyLoadingTableSaysLoading() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(new DefaultTableModel(new Object[]{"A"}, 0));
            JScrollPane scroll = new JScrollPane(table);
            TableEmptyState.install(scroll);
            JPanel overlay = overlay(scroll);
            assertThat(title(overlay)).isEqualTo("Sem registos.");

            List<JTable> loading = TableLoadingState.begin(scroll);

            assertThat(overlay.isVisible()).isTrue();
            assertThat(title(overlay)).isEqualTo(TableEmptyState.LOADING_TEXT);

            TableLoadingState.end(loading);

            assertThat(title(overlay)).isEqualTo("Sem registos.");
            assertThat(TableLoadingState.isLoading(table)).isFalse();
        });
    }

    @Test
    @DisplayName("EC-02: tabela que já tem linhas não é tapada durante a actualização")
    void ec02_tableWithRowsIsNotCovered() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new Object[]{"A"}, 0);
            model.addRow(new Object[]{"x"});
            JTable table = new JTable(model);
            JScrollPane scroll = new JScrollPane(table);
            TableEmptyState.install(scroll);

            TableLoadingState.begin(scroll);

            assertThat(overlay(scroll).isVisible()).isFalse();
        });
    }

    @Test
    @DisplayName("EC-03: cargas em paralelo: só deixa de carregar quando a última termina")
    void ec03_nestedLoadsAreCounted() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable();
            List<JTable> first = TableLoadingState.begin(table);
            List<JTable> second = TableLoadingState.begin(table);
            TableLoadingState.end(first);
            assertThat(TableLoadingState.isLoading(table)).isTrue();
            TableLoadingState.end(second);
            assertThat(TableLoadingState.isLoading(table)).isFalse();
        });
    }

    @Test
    @DisplayName("EC-04: encontra as tabelas dentro de separadores do ecrã que carrega")
    void ec04_findsTablesInsideTabs() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Um", new JScrollPane(new JTable()));
            tabs.addTab("Dois", new JScrollPane(new JTable()));
            JPanel screen = new JPanel();
            screen.add(tabs);
            assertThat(TableLoadingState.tablesIn(screen)).hasSize(2);
        });
    }

    @Test
    @DisplayName("EC-05: UIHelper.loadAsync marca as tabelas do ecrã e liberta-as no fim")
    void ec05_loadAsyncDrivesTheState() throws Exception {
        JTable[] table = new JTable[1];
        JPanel[] screen = new JPanel[1];
        boolean[] loadingDuringFetch = new boolean[1];
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            table[0] = new JTable();
            screen[0] = new JPanel();
            screen[0].add(new JScrollPane(table[0]));
            UIHelper.loadAsync(screen[0], () -> {
                release.await(5, TimeUnit.SECONDS);
                return "ok";
            }, ignored -> finished.countDown(), error -> finished.countDown());
            loadingDuringFetch[0] = TableLoadingState.isLoading(table[0]);
        });
        release.countDown();
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
        SwingUtilities.invokeAndWait(() -> { });
        assertThat(loadingDuringFetch[0]).isTrue();
        assertThat(TableLoadingState.isLoading(table[0])).isFalse();
    }

    @Test
    @DisplayName("EC-06: o loadAsync central chama begin e end do estado de carga")
    void ec06_sourceWiring() throws IOException {
        String src = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/components/UiAsyncTaskSupport.java"));
        assertThat(src).contains("TableLoadingState.begin(scope)").contains("TableLoadingState.end(loadingTables)");
    }

    private static JPanel overlay(JScrollPane scroll) {
        for (Component c : scroll.getComponents()) {
            if (c instanceof JPanel panel && panel.getClientProperty("titleLabel") != null) return panel;
        }
        throw new AssertionError("overlay not installed");
    }

    private static String title(JPanel overlay) {
        return ((JLabel) overlay.getClientProperty("titleLabel")).getText();
    }
}

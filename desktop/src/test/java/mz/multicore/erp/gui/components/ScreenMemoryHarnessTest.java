package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of the screen memory: tables and main window (docs/MEMORIA_ECRA_SPEC.md). */
class ScreenMemoryHarnessTest {

    private Preferences tables;
    private Preferences window;

    @BeforeEach
    void isolatedStores() {
        tables = Preferences.userRoot().node("mz/multicore/erp/test/tables-" + System.nanoTime());
        window = Preferences.userRoot().node("mz/multicore/erp/test/window-" + System.nanoTime());
        TablePrefs.useStore(tables);
        WindowPrefs.useStore(window);
    }

    @AfterEach
    void cleanUp() throws BackingStoreException {
        tables.removeNode();
        window.removeNode();
        TablePrefs.useStore(Preferences.userRoot().node("mz/multicore/erp/ui/tables"));
        WindowPrefs.useStore(Preferences.userRoot().node("mz/multicore/erp/ui/window"));
    }

    @Test
    @DisplayName("ME-01: largura, ordem das colunas e ordenação voltam numa tabela nova com as mesmas colunas")
    void me01_tableLayoutIsRestored() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable first = table();
            first.getColumnModel().moveColumn(2, 0);              // Total first
            first.getColumnModel().getColumn(0).setWidth(240);
            first.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
            TablePrefs.save(first);

            JTable reopened = table();
            TablePrefs.restore(reopened);

            assertThat(reopened.getColumnModel().getColumn(0).getModelIndex()).isEqualTo(2);
            assertThat(reopened.getColumnModel().getColumn(0).getPreferredWidth()).isEqualTo(240);
            List<RowSorter.SortKey> sort = List.copyOf(reopened.getRowSorter().getSortKeys());
            assertThat(sort).containsExactly(new RowSorter.SortKey(1, SortOrder.DESCENDING));
        });
    }

    @Test
    @DisplayName("ME-02: tabelas com colunas diferentes não partilham preferências")
    void me02_differentTablesHaveDifferentKeys() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable orders = table();
            JTable other = new JTable(new DefaultTableModel(new Object[]{"Código", "Nome"}, 0));
            assertThat(TablePrefs.key(orders)).isNotNull().isNotEqualTo(TablePrefs.key(other));
            assertThat(TablePrefs.key(new JTable())).isNull();
        });
    }

    @Test
    @DisplayName("ME-03: \"Repor colunas\" volta à disposição do ecrã e esquece o que estava guardado")
    void me03_resetGoesBackToScreenDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table();
            TablePrefs.install(table);
            assertThat(menuLabels(table)).contains(TablePrefs.RESET_LABEL);

            table.getColumnModel().moveColumn(2, 0);
            TablePrefs.save(table);
            JTable reopened = table();
            TablePrefs.install(reopened);
            // first show captures the screen's layout (model order) and then applies the saved one
            java.lang.reflect.Method once;
            try {
                once = TablePrefs.class.getDeclaredMethod("restoreOnce", JTable.class);
                once.setAccessible(true);
                once.invoke(null, reopened);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
            assertThat(reopened.getColumnModel().getColumn(0).getModelIndex()).isEqualTo(2);

            TablePrefs.reset(reopened);

            assertThat(reopened.getColumnModel().getColumn(0).getModelIndex()).isEqualTo(0);
            assertThat(tables.get(TablePrefs.key(reopened) + ".cols", null)).isNull();
        });
    }

    @Test
    @DisplayName("ME-04: preferências corrompidas são ignoradas sem rebentar o ecrã")
    void me04_corruptPrefsAreIgnored() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table();
            tables.put(TablePrefs.key(table) + ".cols", "x:y,,9:");
            tables.put(TablePrefs.key(table) + ".sort", "nada");
            TablePrefs.restore(table);
            assertThat(table.getColumnModel().getColumn(0).getModelIndex()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("ME-05: a janela principal volta ao tamanho e posição da última sessão; maximizada por omissão")
    void me05_windowBoundsAndState() {
        assertThat(WindowPrefs.savedState()).isEqualTo(Frame.MAXIMIZED_BOTH);
        assertThat(WindowPrefs.savedBounds()).isNull();

        window.putBoolean("maximized", false);
        window.put("bounds", "10,10,1200,800");
        if (WindowPrefs.fitsAScreen(new Rectangle(10, 10, 1200, 800))) {
            assertThat(WindowPrefs.savedBounds()).isEqualTo(new Rectangle(10, 10, 1200, 800));
            assertThat(WindowPrefs.savedState()).isEqualTo(Frame.NORMAL);
        }

        window.put("bounds", "-90000,-90000,1200,800");
        assertThat(WindowPrefs.savedBounds()).as("fora de qualquer ecrã").isNull();
        assertThat(WindowPrefs.savedState()).isEqualTo(Frame.MAXIMIZED_BOTH);
    }

    @Test
    @DisplayName("ME-06: instalado centralmente pelo styleTable e pelo arranque da janela principal")
    void me06_wiring() throws IOException {
        assertThat(Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/components/UiTabbedAndTableStyler.java")))
                .contains("TablePrefs.install(table);");
        assertThat(Files.readString(Path.of("src/main/java/mz/multicore/erp/desktop/DesktopLauncher.java")))
                .contains("WindowPrefs.savedBounds()").contains("WindowPrefs.save(mainFrame)");
    }

    private static JTable table() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Data", "Cliente", "Total"}, 0);
        model.addRow(new Object[]{"01/01/2026", "Ana", 10});
        JTable table = new JTable(model);
        table.setRowSorter(new TableRowSorter<>(model));
        return table;
    }

    private static List<String> menuLabels(JTable table) {
        return java.util.Arrays.stream(table.getTableHeader().getComponentPopupMenu().getComponents())
                .filter(c -> c instanceof JMenuItem).map(c -> ((JMenuItem) c).getText()).toList();
    }
}

package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uma acção age sobre a linha que o operador seleccionou, nunca sobre a que está na mesma posição
 * do modelo. Filtro, ordenação e paginação ({@link ClientTablePagination}) separam os dois índices.
 * Ver {@code docs/SELECCAO_LINHA_MODELO_SPEC.md}.
 */
class SelectedRowModelIndexHarnessTest {

    private static final Pattern SELECTED = Pattern.compile("int (\\w+) = [\\w.]+\\.getSelectedRow\\(\\);");

    @Test // SL-01
    void comFiltroALinhaSeleccionadaEADoModelo() {
        JTable table = table(3);
        JTextField search = TableFilter.searchField("Filtrar…");
        TableFilter.install(table, search);
        search.setText("Ana");
        table.setRowSelectionInterval(0, 0);

        assertThat(table.getSelectedRow()).isZero();
        assertThat(TableFilter.selectedModelRow(table)).as("a Ana é a 3.ª do modelo").isEqualTo(2);
    }

    @Test // SL-02
    void naSegundaPaginaALinhaSeleccionadaEADoModelo() {
        JTable table = table(60);
        javax.swing.JPanel pager = ClientTablePagination.install(table);
        nextPage(pager).doClick();
        table.setRowSelectionInterval(0, 0);

        assertThat(table.getSelectedRow()).isZero();
        assertThat(TableFilter.selectedModelRow(table)).as("1.ª linha da 2.ª página").isEqualTo(50);
    }

    @Test // SL-03
    void nenhumaAccaoUsaOIndiceDaVistaNoModeloOuNaLista() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/mz/multicore/erp/gui"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    Matcher m = SELECTED.matcher(lines.get(i));
                    if (!m.find()) continue;
                    String var = m.group(1);
                    String window = String.join("\n", lines.subList(i, Math.min(lines.size(), i + 12)));
                    if (window.contains("convertRowIndexToModel")) continue;
                    Pattern use = Pattern.compile("[Mm]odel\\w*\\.(getValueAt|removeRow|setValueAt)\\(" + var
                            + "\\b|getModel\\(\\)\\.getValueAt\\(" + var + "\\b|\\.get\\(" + var + "\\)");
                    if (use.matcher(window).find()) violations.add(file.getFileName() + ":" + (i + 1));
                }
            }
        }
        assertThat(violations).as("usar TableFilter.selectedModelRow(table)").isEmpty();
    }

    private static javax.swing.JButton nextPage(java.awt.Container root) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JButton b
                    && "Página seguinte".equals(b.getAccessibleContext().getAccessibleName())) return b;
            if (c instanceof java.awt.Container child) {
                try { return nextPage(child); } catch (AssertionError ignored) { }
            }
        }
        throw new AssertionError("Botão não encontrado: Página seguinte");
    }

    private static JTable table(int rows) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Nome"}, 0);
        for (int i = 0; i < rows; i++) model.addRow(new Object[]{i == 2 ? "Ana" : "Cliente " + i});
        return new JTable(model);
    }
}

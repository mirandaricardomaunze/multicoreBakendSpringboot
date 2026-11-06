package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Painel de espreitar (tecla Espaço) repete o ecrã. Ver {@code FORMATO_NUMERICO_SPEC} FN-09 e FN-10.
 */
class QuickPeekCellTextHarnessTest {

    private static final Pattern RAW_PEEK_VALUE =
            Pattern.compile("PeekItem\\([^\\n]*String\\.valueOf\\([\\w.()]*getValueAt\\(");
    private static final Pattern DOUBLE_CONVERSION =
            Pattern.compile("\\(\\w+, mr\\) ->[^\\n]*\\n[^\\n]*convertRowIndexToModel\\(mr\\)|convertRowIndexToModel\\(mr\\)\\)\\);");

    @Test // FN-09
    void oPainelMostraOValorComoAColuna() {
        JTable table = table();
        QuickPeekPanel peek = TableQuickPeekController.install(table, new JPanel(new BorderLayout())).getPeekPanel();

        assertThat(peek.cell(new BigDecimal("1460.00"), 1)).isEqualTo("1.460,00 MT");
        assertThat(peek.cell(new BigDecimal("4.000"), 2)).isEqualTo("4,00");
        assertThat(CellText.plain(new BigDecimal("1460.5"))).isEqualTo("1.460,50");
        assertThat(CellText.plain(7L)).as("identificadores ficam intactos").isEqualTo("7");
        assertThat(CellText.plain(null)).isEqualTo("—");
    }

    @Test // FN-10
    void comFiltroOPainelMostraALinhaSeleccionadaENaoOutra() {
        JTable table = table();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter("FT-2", 0)); // fica só a 2.ª linha do modelo

        AtomicInteger received = new AtomicInteger(-1);
        TableQuickPeekController.install(table, new JPanel(new BorderLayout()), (p, mr) -> received.set(mr))
                .refresh(0);

        assertThat(received.get()).as("o populador recebe a linha do modelo").isEqualTo(1);
    }

    @Test // FN-09 / FN-10 — nenhum ecrã volta ao valor cru nem converte a linha duas vezes
    void nenhumEcraUsaValorCruNemConverteALinhaDuasVezes() throws IOException {
        List<String> raw = new ArrayList<>();
        List<String> doubled = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/mz/multicore/erp/gui"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                if (RAW_PEEK_VALUE.matcher(source).find()) raw.add(file.getFileName().toString());
                if (DOUBLE_CONVERSION.matcher(source).find()) doubled.add(file.getFileName().toString());
            }
        }
        assertThat(raw).as("usar peek.cell(valor, coluna)").isEmpty();
        assertThat(doubled).as("o controlador já entrega a linha do modelo").isEmpty();
    }

    private static JTable table() {
        DefaultTableModel model = new DefaultTableModel(new Object[][]{
                {"FT-1", new BigDecimal("1460.00"), new BigDecimal("4.000")},
                {"FT-2", new BigDecimal("99.50"), new BigDecimal("1.000")},
        }, new String[]{"Nº", "Total", "Qtd"});
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.quantity());
        return table;
    }
}

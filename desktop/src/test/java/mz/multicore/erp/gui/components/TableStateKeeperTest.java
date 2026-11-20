package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.assertj.core.api.Assertions.assertThat;

/** AA-08 — see docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §4.4. */
class TableStateKeeperTest {

    private static DefaultTableModel model(String... numbers) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Tipo", "Nº", "Cliente"}, 0);
        for (String number : numbers) model.addRow(new Object[]{"Fatura", number, "Cliente " + number});
        return model;
    }

    @Test
    void aa08_chaveVemDaColunaNumeroEnaoDaPosicao() {
        assertThat(TableStateKeeper.keyColumn(model())).isEqualTo(1);
        DefaultTableModel noKey = new DefaultTableModel(new Object[]{"Nome", "Valor"}, 0);
        assertThat(TableStateKeeper.keyColumn(noKey)).isZero();
    }

    @Test
    void aa08_linhaSeleccionadaVoltaMesmoQueMudeDePosicao() {
        JTable table = new JTable(model("FT-1", "FT-2", "FT-3"));
        table.setRowSelectionInterval(1, 1);
        TableStateKeeper.Snapshot snapshot = TableStateKeeper.capture(table);
        assertThat(snapshot.selectedKey()).isEqualTo("FT-2");

        table.setModel(model("FT-9", "FT-1", "FT-2", "FT-3"));
        TableStateKeeper.restore(table, snapshot);

        assertThat(table.getSelectedRow()).isEqualTo(2);
        assertThat(table.getValueAt(table.getSelectedRow(), 1)).isEqualTo("FT-2");
    }

    @Test
    void aa08_linhaQueDesapareceuFicaSemSeleccao() {
        JTable table = new JTable(model("FT-1", "FT-2"));
        table.setRowSelectionInterval(0, 0);
        TableStateKeeper.Snapshot snapshot = TableStateKeeper.capture(table);

        table.setModel(model("FT-2", "FT-3"));
        table.setRowSelectionInterval(1, 1);
        TableStateKeeper.restore(table, snapshot);

        assertThat(table.getSelectedRow()).isEqualTo(-1);
    }

    @Test
    void aa08_paginaDaPaginacaoDeClienteEReposta() {
        String[] many = new String[120];
        for (int i = 0; i < many.length; i++) many[i] = "FT-" + i;
        JTable table = new JTable(model(many));
        ClientTablePagination.install(table);
        ClientTablePagination.showPage(table, 2);
        TableStateKeeper.Snapshot snapshot = TableStateKeeper.capture(table);
        assertThat(snapshot.page()).isEqualTo(2);

        ClientTablePagination.showPage(table, 0);
        TableStateKeeper.restore(table, snapshot);

        assertThat(ClientTablePagination.currentPage(table)).isEqualTo(2);
    }
}

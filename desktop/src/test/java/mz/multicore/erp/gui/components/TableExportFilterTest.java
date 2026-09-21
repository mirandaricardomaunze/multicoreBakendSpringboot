package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.printing.dto.TableExportRequest;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uma exportação leva <b>o que o operador filtrou</b>.
 *
 * <p>Exportar só a página que está à vista mentiria sobre o tamanho da lista; exportar o modelo
 * inteiro ignoraria o filtro que ele acabou de escrever. Ambos os erros são silenciosos — o PDF
 * sai, com o conteúdo errado — e por isso ficam presos aqui.</p>
 *
 * <p>Quem <b>desenha</b> o PDF é o servidor ({@code TableExportPrintService}), porque é lá que
 * vivem o nome, o NUIT e a morada da empresa. Aqui prende-se o que o desktop lhe manda.</p>
 */
class TableExportFilterTest {

    @Test // CE-01
    void aExportacaoLevaSoAsLinhasQuePassamOFiltro() {
        JTable table = clientsTable();
        JTextField search = TableFilter.searchField("Filtrar…");
        TableFilter.install(table, search);

        assertThat(ClientTablePagination.filteredModelRows(table)).hasSize(3);

        search.setText("Maputo");
        List<Integer> filtered = ClientTablePagination.filteredModelRows(table);
        assertThat(filtered).containsExactly(0, 2);
    }

    @Test // CE-02
    void semFiltroNenhumVaiOModeloInteiro() {
        JTable table = clientsTable();
        assertThat(ClientTablePagination.filteredModelRows(table)).containsExactly(0, 1, 2);
    }

    @Test // CE-03
    void aPaginacaoNaoEncolheAExportacao() {
        JTable table = clientsTable();
        JTextField search = TableFilter.searchField("Filtrar…");
        TableFilter.install(table, search);
        ClientTablePagination.install(table);

        // A tabela mostra uma página de cada vez; a exportação continua a levar a lista toda.
        assertThat(ClientTablePagination.filteredModelRows(table))
                .as("a exportação não é a página")
                .containsExactly(0, 1, 2);
    }

    @Test // CE-04
    void oPedidoAoServidorLevaSoAsLinhasPedidas() {
        JTable table = clientsTable();

        TableExportRequest everything =
                TableExportAction.snapshot("Clientes", table, List.of(0, 1, 2));
        TableExportRequest onlyOne =
                TableExportAction.snapshot("Clientes", table, List.of(1));

        assertThat(everything.headers())
                .containsExactly("ID", "Nome", "NUIT / NIF", "Email", "Endereço");
        assertThat(everything.rows()).hasSize(3);
        assertThat(everything.rows().get(0)).contains("Maria Cossa");

        assertThat(onlyOne.rows()).hasSize(1);
        assertThat(onlyOne.rows().get(0)).contains("João Nhaca");
        assertThat(onlyOne.rows().toString())
                .as("as linhas não pedidas não sobem para o servidor")
                .doesNotContain("Maria Cossa", "Ana Timba");
    }

    @Test // CE-06
    void asCelulasSobemJaFormatadasEUmaCelulaVaziaNaoEnula() {
        DefaultTableModel model = (DefaultTableModel) clientsTable().getModel();
        model.setValueAt(null, 1, 3);
        JTable table = new JTable(model);

        TableExportRequest request = TableExportAction.snapshot("Clientes", table, List.of(1));

        assertThat(request.rows().get(0).get(0)).as("o que está no ecrã é texto").isEqualTo("2");
        assertThat(request.rows().get(0).get(3)).as("célula vazia é vazia, não nula").isEmpty();
    }

    @Test // CE-05
    void oEcraDeClientesExportaOQueEstaFiltrado() throws Exception {
        Path panel = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui",
                "ClientesPanel.java");
        String source = Files.readString(panel);
        assertThat(source).as("o botão existe").contains("Exportar PDF", "fas-file-pdf");
        assertThat(source).as("a exportação passa pelo sítio único")
                .contains("TableExportAction.export(this, printApiClient, table, \"Clientes\", \"clientes\")");

        Path action = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "components",
                "TableExportAction.java");
        String actionSource = Files.readString(action);
        assertThat(actionSource).as("leva o que está filtrado")
                .contains("ClientTablePagination.filteredModelRows(table)");
        assertThat(actionSource).as("e passa pelo modal de impressão")
                .contains("PrintPreviewDialog.show(");
        assertThat(actionSource).as("quem desenha o PDF é o servidor")
                .contains("printApiClient.renderTable(");
    }

    private static JTable clientsTable() {
        String[] columns = {"ID", "Nome", "NUIT / NIF", "Email", "Endereço"};
        Object[][] rows = {
                {1L, "Maria Cossa", "400111222", "maria@exemplo.mz", "Av. Julius Nyerere, Maputo"},
                {2L, "João Nhaca", "400333444", "joao@exemplo.mz", "Rua da Beira, Sofala"},
                {3L, "Ana Timba", "400555666", "ana@exemplo.mz", "Bairro Central, Maputo"},
        };
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        return new JTable(model);
    }
}

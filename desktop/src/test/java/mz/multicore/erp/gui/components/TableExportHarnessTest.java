package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness canónico para validação do motor universal de exportação tabular (TableDataExporter).
 * Cobre exportação CSV, Excel XML Spreadsheet 2003, preservação de tipos numéricos e respeito a filtros.
 */
class TableExportHarnessTest {

    @Test
    @DisplayName("TE-01: Exportação CSV gera delimitadores ';' e escapa aspas e caracteres especiais")
    void exportacaoCsvGeraDelimitadoresEEscapaValores() {
        String[] cols = {"Código", "Descrição", "Preço", "Notas"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        model.addRow(new Object[]{"ART-001", "Arroz Agulha 5kg", "550,00 MT", "Promoção especial"});
        model.addRow(new Object[]{"ART-002", "Açúcar Branco; 1kg", "90,00 MT", "Contém \"lote\" A"});

        JTable table = new JTable(model);

        String csv = TableDataExporter.toCsvString(table);

        assertThat(csv)
                .contains("Código;Descrição;Preço;Notas")
                .contains("ART-001;Arroz Agulha 5kg;\"550,00 MT\";Promoção especial")
                // Ponto e vírgula na descrição deve ser delimitado por aspas
                .contains("\"Açúcar Branco; 1kg\"")
                // Aspas internas devem ser duplicadas conforme RFC 4180
                .contains("\"Contém \"\"lote\"\" A\"");
    }

    @Test
    @DisplayName("TE-02: Exportação Excel XML Spreadsheet 2003 gera tipos Number e cabeçalhos em negrito")
    void exportacaoExcelXmlGeraTiposCorretos() {
        String[] cols = {"Artigo", "Quantidade", "Preço", "Subtotal"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        model.addRow(new Object[]{"Óleo de Cozinha 1L", 5, new BigDecimal("120.50"), "602,50 MT"});

        JTable table = new JTable(model);

        String xml = TableDataExporter.toExcelXml(table, "Lista de Vendas");

        assertThat(xml)
                .contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .contains("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"")
                .contains("<Worksheet ss:Name=\"Lista de Vendas\">")
                .contains("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Artigo</Data></Cell>")
                .contains("<Data ss:Type=\"String\">Óleo de Cozinha 1L</Data>")
                .contains("<Data ss:Type=\"Number\">5</Data>")
                .contains("<Data ss:Type=\"Number\">120.50</Data>")
                .contains("<Data ss:Type=\"Number\">602.50</Data>");
    }

    @Test
    @DisplayName("TE-03: Exportação respeita filtros activos e exporta apenas linhas correspondentes")
    void exportacaoRespeitaFiltrosActivos() {
        String[] cols = {"Cliente", "Cidade", "Saldo"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        model.addRow(new Object[]{"Empresa Alpha", "Maputo", "10.000,00 MT"});
        model.addRow(new Object[]{"Empresa Beta", "Beira", "5.000,00 MT"});
        model.addRow(new Object[]{"Empresa Gama", "Maputo", "2.500,00 MT"});

        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        // Filtrar apenas clientes de "Maputo"
        sorter.setRowFilter(RowFilter.regexFilter("Maputo", 1));
        table.setRowSorter(sorter);

        String csv = TableDataExporter.toCsvString(table);

        assertThat(csv)
                .contains("Empresa Alpha")
                .contains("Empresa Gama")
                .doesNotContain("Empresa Beta");
    }

    @Test
    @DisplayName("TE-04: Tabela nula ou vazia não lança excepções")
    void tabelaNulaOuVaziaNaoLancaExcepcoes() {
        assertThat(TableDataExporter.toCsvString(null)).isEmpty();
        assertThat(TableDataExporter.toExcelXml(null, "Teste")).isEmpty();

        DefaultTableModel emptyModel = new DefaultTableModel(new String[]{"Col1", "Col2"}, 0);
        JTable emptyTable = new JTable(emptyModel);

        String csv = TableDataExporter.toCsvString(emptyTable);
        assertThat(csv).contains("Col1;Col2"); // Apenas cabeçalho
    }
}

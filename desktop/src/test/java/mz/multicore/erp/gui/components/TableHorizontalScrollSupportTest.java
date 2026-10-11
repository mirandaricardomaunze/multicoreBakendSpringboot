package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do suporte DRY de rolagem horizontal em tabelas com muitas colunas.
 */
class TableHorizontalScrollSupportTest {

    @Test
    @DisplayName("HORIZ-01: Tabela com muitas colunas ativa AUTO_RESIZE_OFF quando excede o viewport")
    void testManyColumnsEnablesHorizontalScrollWhenExceedsViewport() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String[] cols = {"Tipo", "Nº", "Cliente / Destino", "Artigos", "Qtd", "Armazém", "Data", "Estado", "Total"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);
            JTable table = new JTable(model);
            UIHelper.styleTable(table);

            JScrollPane scroll = new JScrollPane(table);
            UIHelper.styleScrollPane(scroll);

            scroll.setSize(new Dimension(500, 300));
            scroll.getViewport().setSize(new Dimension(500, 260));

            TableHorizontalScrollSupport.update(table);

            assertEquals(JTable.AUTO_RESIZE_OFF, table.getAutoResizeMode(),
                    "Tabela com 9 colunas e viewport estreito (500px) deve usar AUTO_RESIZE_OFF para permitir scroll horizontal");
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED, scroll.getHorizontalScrollBarPolicy());
            assertTrue(scroll.getHorizontalScrollBar().getUI() instanceof SlimScrollBarUI);
        });
    }

    @Test
    @DisplayName("HORIZ-02: Tabela com poucas colunas que cabe no viewport usa AUTO_RESIZE_SUBSEQUENT_COLUMNS")
    void testFewColumnsKeepsSubsequentColumnsWhenFitting() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String[] cols = {"ID", "Nome", "Activo"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);
            JTable table = new JTable(model);
            UIHelper.styleTable(table);

            JScrollPane scroll = new JScrollPane(table);
            UIHelper.styleScrollPane(scroll);

            scroll.setSize(new Dimension(1000, 300));
            scroll.getViewport().setSize(new Dimension(1000, 260));

            TableHorizontalScrollSupport.update(table);

            assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, table.getAutoResizeMode(),
                    "Tabela com poucas colunas que cabe com folga deve preencher 100% da largura");
        });
    }

    @Test
    @DisplayName("HORIZ-03: UIHelper.ensureHeadersFit calibra mínimos e preserva legibilidade")
    void testEnsureHeadersFitCalibratesMinimums() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String[] cols = {"Descrição Muito Longa da Mercadoria", "Valor Total"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);
            JTable table = new JTable(model);
            UIHelper.styleTable(table);

            UIHelper.ensureHeadersFit(table);

            assertTrue(table.getColumnModel().getColumn(0).getPreferredWidth() >= 100,
                    "Coluna com título longo deve ter largura preferida suficiente para o cabeçalho");
        });
    }

    @Test
    @DisplayName("HORIZ-04: Instalação DRY via UIHelper.styleScrollPane detecta tabela e configura scroll")
    void testDryInstallationViaStyleScrollPane() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String[] cols = {"C1", "C2", "C3", "C4", "C5", "C6", "C7"};
            JTable table = new JTable(new DefaultTableModel(cols, 0));
            JScrollPane scroll = new JScrollPane(table);

            UIHelper.styleScrollPane(scroll);

            assertTrue(Boolean.TRUE.equals(table.getClientProperty(TableHorizontalScrollSupport.INSTALLED_KEY)),
                    "TableHorizontalScrollSupport deve ser instalado automaticamente via styleScrollPane");
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED, scroll.getHorizontalScrollBarPolicy());
        });
    }
}

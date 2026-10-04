package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validação de conformidade canónica para precisão decimal de tabelas:
 * Regra: todas as tabelas devem apresentar números decimais com exactamente 2 casas decimais.
 */
class TableDecimalPrecisionHarnessTest {

    @Test
    @DisplayName("TableCellRenderers.quantity deve formatar com exactamente 2 casas decimais")
    void quantityRenderer_formataComDuasCasasDecimais() {
        JTable table = new JTable(1, 1);
        TableCellRenderer renderer = TableCellRenderers.quantity();

        JLabel lbl1 = (JLabel) renderer.getTableCellRendererComponent(
                table, new BigDecimal("2.5"), false, false, 0, 0);
        assertEquals("2,50", lbl1.getText(), "2.5 deve renderizar como 2,50");

        JLabel lbl2 = (JLabel) renderer.getTableCellRendererComponent(
                table, new BigDecimal("1250.789"), false, false, 0, 0);
        assertEquals("1 250,79", lbl2.getText(), "1250.789 deve arredondar para 1 250,79");

        JLabel lbl3 = (JLabel) renderer.getTableCellRendererComponent(
                table, 15.0, false, false, 0, 0);
        assertEquals("15,00", lbl3.getText(), "Double 15.0 deve renderizar como 15,00");

        JLabel lbl4 = (JLabel) renderer.getTableCellRendererComponent(
                table, "3.500", false, false, 0, 0);
        assertEquals("3,50", lbl4.getText(), "String 3.500 deve renderizar como 3,50");
    }

    @Test
    @DisplayName("TableCellRenderers.money deve formatar com exactamente 2 casas decimais e sufixo MT")
    void moneyRenderer_formataComDuasCasasDecimais() {
        JTable table = new JTable(1, 1);
        TableCellRenderer renderer = TableCellRenderers.money();

        JLabel lbl = (JLabel) renderer.getTableCellRendererComponent(
                table, new BigDecimal("99.9"), false, false, 0, 0);
        assertEquals("99,90 MT", lbl.getText());
    }

    @Test
    @DisplayName("UIHelper.styleTable default renderer formata números com 2 casas decimais")
    void styleTable_defaultRenderer_formataDecimaisComDuasCasas() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "Preço", "Qtd Double", "Qtd Float", "String Dec", "String Assinada"}, 0);
        model.addRow(new Object[]{
                101L,
                new BigDecimal("45.1"),
                12.5,
                7.8f,
                "125.600",
                "+3.400"
        });

        JTable table = new JTable(model);
        UIHelper.styleTable(table);

        // BigDecimal (Preço)
        ComponentCellRendererAssert(table, 0, 1, "45,10");
        // Double (Qtd Double)
        ComponentCellRendererAssert(table, 0, 2, "12,50");
        // Float (Qtd Float)
        ComponentCellRendererAssert(table, 0, 3, "7,80");
        // String decimal
        ComponentCellRendererAssert(table, 0, 4, "125,60");
        // String com sinal positivo
        ComponentCellRendererAssert(table, 0, 5, "+3,40");
    }

    private static void ComponentCellRendererAssert(JTable table, int row, int col, String expected) {
        TableCellRenderer renderer = table.getCellRenderer(row, col);
        JLabel lbl = (JLabel) renderer.getTableCellRendererComponent(
                table, table.getValueAt(row, col), false, false, row, col);
        assertEquals(expected, lbl.getText());
    }
}

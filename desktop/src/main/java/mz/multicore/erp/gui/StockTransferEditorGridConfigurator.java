package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.gui.components.QuantityField;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.List;

import static mz.multicore.erp.gui.StockTransferEditorRowCalculator.*;

/** Configurador de colunas, editores e renderizadores visuais da grelha de transferência. */
final class StockTransferEditorGridConfigurator {

    private StockTransferEditorGridConfigurator() {}

    static void configure(JTable linesTable, ProductSearchComboBox productCellEditor) {
        linesTable.getColumnModel().getColumn(COL_PRODUCT).setCellEditor(ProductSearchComboBox.createTableCellEditor(productCellEditor));
        linesTable.getColumnModel().getColumn(COL_PRODUCT).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof ProductDTO product ? product.name() : "Pesquisar produto...");
            }
        });
        for (int col : List.of(COL_QUANTITY, COL_PACKAGES, COL_BOXES)) {
            linesTable.getColumnModel().getColumn(col).setCellEditor(new DefaultCellEditor(new QuantityField("0", false)));
            linesTable.getColumnModel().getColumn(col).setCellRenderer(TableCellRenderers.quantity());
        }
        linesTable.getColumnModel().getColumn(COL_UNIT_PRICE).setCellRenderer(TableCellRenderers.money());
        linesTable.getColumnModel().getColumn(COL_TOTAL).setCellRenderer(TableCellRenderers.money());
        linesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        linesTable.setSurrendersFocusOnKeystroke(true);
        linesTable.setToolTipText("Edite directamente Produto, Qtd, Emb. ou Cx.; os restantes campos são derivados.");

        linesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {260, 100, 80, 80, 70, 110, 120, 100, 70, 120};
        for (int i = 0; i < widths.length; i++) {
            linesTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }
}

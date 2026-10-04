package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de teste para navegação fluida por células estilo Excel/PHC na grelha documental.
 */
class GridCellNavigationHarnessTest {

    @Test
    @DisplayName("GCN-01: findNextEditableColumn e findPrevEditableColumn localizam colunas editáveis")
    void testFindEditableColumns() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Produto", "Qtd", "Total"}, 1) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 1 || c == 2; // apenas Produto e Qtd são editáveis
            }
        };
        JTable table = new JTable(model);

        assertEquals(1, UIHelper.findNextEditableColumn(table, 0, -1), "Primeira editável deve ser 1");
        assertEquals(2, UIHelper.findNextEditableColumn(table, 0, 1), "Próxima editável a partir de 1 deve ser 2");
        assertEquals(-1, UIHelper.findNextEditableColumn(table, 0, 2), "Após 2 não há mais colunas editáveis");

        assertEquals(2, UIHelper.findPrevEditableColumn(table, 0, 4), "Última editável antes de 4 deve ser 2");
        assertEquals(1, UIHelper.findPrevEditableColumn(table, 0, 2), "Anterior a 2 deve ser 1");
        assertEquals(-1, UIHelper.findPrevEditableColumn(table, 0, 1), "Antes de 1 não há mais colunas editáveis");
    }

    @Test
    @DisplayName("GCN-02: installCellNavigationKeys regista acções TAB e ENTER")
    void testNavigationKeysRegistration() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ColA", "ColB"}, 1);
        JTable table = new JTable(model);

        AtomicBoolean addCalled = new AtomicBoolean(false);
        UIHelper.installCellNavigationKeys(table, () -> addCalled.set(true));

        assertTrue(table.getActionMap().get("gridNavNext") != null, "Ação gridNavNext deve estar registrada");
        assertTrue(table.getActionMap().get("gridNavPrev") != null, "Ação gridNavPrev deve estar registrada");
    }
}

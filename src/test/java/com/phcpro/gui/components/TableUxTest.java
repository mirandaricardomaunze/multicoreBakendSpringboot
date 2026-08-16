package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes da lógica pura do lote de UX das tabelas: estado vazio ({@link TableEmptyState}) e menu de
 * contexto ({@link TableContextMenu}). Molde: {@code TableFilterTest}. Cenários UX-03..UX-06.
 */
class TableUxTest {

    @Test // UX-03
    void emptyState_textoPorOmissao() {
        JTable table = new JTable();
        assertEquals("Sem registos.", TableEmptyState.resolveText(table));
    }

    @Test // UX-04
    void emptyState_textoPersonalizado() {
        JTable table = new JTable();
        table.putClientProperty("emptyText", "Sem encomendas.");
        assertEquals("Sem encomendas.", TableEmptyState.resolveText(table));
    }

    @Test // UX-04b
    void emptyState_textoVazioCaiNoDefault() {
        JTable table = new JTable();
        table.putClientProperty("emptyText", "   ");
        assertEquals("Sem registos.", TableEmptyState.resolveText(table));
    }

    @Test // regressão: o estado vazio nunca pode ficar sobre linhas existentes
    void emptyState_desapareceQuandoTabelaRecebeRegistos() throws Exception {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Nome"}, 0);
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);

        SwingUtilities.invokeAndWait(() -> TableEmptyState.install(scroll));
        assertTrue(emptyStateLabel(scroll).isVisible());

        SwingUtilities.invokeAndWait(() -> model.addRow(new Object[]{"Registo existente"}));
        SwingUtilities.invokeAndWait(() -> { }); // processa a confirmação diferida do estado

        assertEquals(1, table.getRowCount());
        assertFalse(emptyStateLabel(scroll).isVisible());
    }

    private static javax.swing.JLabel emptyStateLabel(JScrollPane scroll) {
        for (Component component : scroll.getComponents()) {
            if (component instanceof javax.swing.JLabel label) return label;
        }
        fail("Overlay do estado vazio não instalado.");
        return null;
    }

    @Test // UX-05
    void contextMenu_rowToText_separaPorTab_nulosVazios() {
        assertEquals("A\t2\t", TableContextMenu.rowToText(new Object[]{"A", 2, null}));
    }

    @Test // UX-06
    void contextMenu_cellToText() {
        assertEquals("", TableContextMenu.cellToText(null));
        assertEquals("FT-2026/5", TableContextMenu.cellToText("FT-2026/5"));
        assertEquals("42", TableContextMenu.cellToText(42));
    }
}

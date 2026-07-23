package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;

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

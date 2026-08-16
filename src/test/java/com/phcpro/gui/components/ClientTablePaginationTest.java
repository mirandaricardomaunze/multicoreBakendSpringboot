package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Component;
import java.awt.Container;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTablePaginationTest {

    @Test
    void pagesListingAndPreservesFunctionalFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"Nome"}, 0);
            for (int i = 0; i < 60; i++) model.addRow(new Object[]{"Produto " + i});
            JTable table = new JTable(model);
            table.setRowSorter(new TableRowSorter<>(model));

            JPanel pager = ClientTablePagination.install(table);
            assertThat(table.getRowCount()).isEqualTo(50);
            findButton(pager, "Página seguinte").doClick();
            assertThat(table.getRowCount()).isEqualTo(10);

            ClientTablePagination.setBaseFilter(table, new RowFilter<>() {
                @Override public boolean include(Entry<? extends javax.swing.table.TableModel,
                        ? extends Integer> entry) {
                    return entry.getIdentifier() < 7;
                }
            });
            assertThat(table.getRowCount()).isEqualTo(7);
            assertThat(findButton(pager, "Página anterior").isEnabled()).isFalse();
        });
    }

    private static JButton findButton(Container root, String accessibleName) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button
                    && accessibleName.equals(button.getAccessibleContext().getAccessibleName())) return button;
            if (component instanceof Container child) {
                JButton found = findButtonOrNull(child, accessibleName);
                if (found != null) return found;
            }
        }
        throw new AssertionError("Botão não encontrado: " + accessibleName);
    }

    private static JButton findButtonOrNull(Container root, String accessibleName) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button
                    && accessibleName.equals(button.getAccessibleContext().getAccessibleName())) return button;
            if (component instanceof Container child) {
                JButton found = findButtonOrNull(child, accessibleName);
                if (found != null) return found;
            }
        }
        return null;
    }
}

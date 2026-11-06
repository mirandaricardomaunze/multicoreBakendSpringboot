package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitários comuns de UI e formatação para o módulo de Contabilidade.
 */
public final class AccountingUiHelper {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AccountingUiHelper() {}

    public static DefaultTableModel createTableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable styledTable(DefaultTableModel model, boolean disablePagination) {
        JTable table = new JTable(model);
        if (disablePagination) {
            table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        }
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    public static void applyMoneyRenderers(JTable table, int... columns) {
        for (int column : columns) {
            table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
    }

    public static ModernPanel createTableCard(JTable table) {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            peek.setHeaderIcon("fas-book", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(table.getModel().getValueAt(row, 0)));
            if (table.getModel().getColumnCount() > 1) {
                peek.setSubtitle(String.valueOf(table.getModel().getValueAt(row, 1)));
            }
            List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
            for (int col = 0; col < table.getModel().getColumnCount(); col++) {
                items.add(new QuickPeekPanel.PeekItem(
                        table.getModel().getColumnName(col),
                        String.valueOf(table.getModel().getValueAt(row, col)),
                        col == table.getModel().getColumnCount() - 1
                ));
            }
            peek.setItems(items);
        });

        return card;
    }

    public static JPanel wrapTabCard(JPanel card) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 5, 5, 5));
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    public static JLabel createMutedLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(UIHelper.TEXT_LIGHT);
        label.setBorder(new EmptyBorder(10, 2, 0, 2));
        return label;
    }

    public static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Data inválida — use aaaa-mm-dd.");
        }
    }
}

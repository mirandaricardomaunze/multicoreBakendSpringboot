package com.phcpro.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import java.awt.Font;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Set;

/**
 * Rodapé de tabela de listagem: mostra a <b>contagem de linhas visíveis</b> e, quando a tabela tem
 * uma coluna de valor monetário (Total/Valor/Em Dívida/…), a <b>soma</b>. Acompanha o filtro
 * (RowSorter) e alterações ao modelo. Uma fonte de verdade para o "N registo(s) · Total: X MT" por
 * baixo das tabelas — em vez de cada painel calcular o seu.
 */
public final class TableFooter {

    private TableFooter() {}

    /** Cabeçalhos que representam um valor a somar (evita somar "Preço"/"IVA%" — sem sentido). */
    private static final Set<String> MONEY_COLUMNS = Set.of(
            "total", "valor", "em dívida", "em divida", "montante", "saldo", "subtotal", "mensalidade");

    /** Instala e devolve o rótulo de rodapé (soma a 1ª coluna monetária detectada, se houver). */
    public static JLabel install(JTable table) {
        return install(table, detectMoneyColumn(table));
    }

    /** Como acima, mas somando a coluna indicada (índice), ou {@code -1} para só contagem. */
    public static JLabel install(JTable table, int moneyColumn) {
        JLabel label = new JLabel(" ");
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(6, 4, 0, 4)));

        Runnable update = () -> label.setText(render(table, moneyColumn));
        table.getModel().addTableModelListener(e -> update.run());
        table.addPropertyChangeListener("model", e -> {
            if (e.getNewValue() instanceof TableModel m) m.addTableModelListener(ev -> update.run());
            update.run();
        });
        if (table.getRowSorter() != null) {
            table.getRowSorter().addRowSorterListener(e -> update.run());
        }
        table.addPropertyChangeListener("rowSorter", e -> {
            if (table.getRowSorter() != null) table.getRowSorter().addRowSorterListener(ev -> update.run());
            update.run();
        });
        update.run();
        return label;
    }

    private static int detectMoneyColumn(JTable table) {
        TableModel m = table.getModel();
        for (int c = 0; c < m.getColumnCount(); c++) {
            String name = m.getColumnName(c);
            if (name != null && MONEY_COLUMNS.contains(name.trim().toLowerCase())) return c;
        }
        return -1;
    }

    private static String render(JTable table, int moneyColumn) {
        int rows = table.getRowCount(); // linhas visíveis (após filtro)
        String count = rows + (rows == 1 ? " registo" : " registos");
        if (moneyColumn < 0 || moneyColumn >= table.getColumnCount()) {
            return count;
        }
        BigDecimal sum = BigDecimal.ZERO;
        boolean any = false;
        for (int r = 0; r < rows; r++) {
            BigDecimal n = parseMoney(String.valueOf(table.getValueAt(r, moneyColumn)));
            if (n != null) { sum = sum.add(n); any = true; }
        }
        return any ? count + " · Total: " + String.format("%,.2f MT", sum) : count;
    }

    /** Extrai um valor monetário de um texto formatado ("1.234,56 MT"), tolerando moeda/agrupamento. */
    static BigDecimal parseMoney(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("(?i)\\s*(MT|MZN|MTn|€|\\$)\\s*$", "").trim();
        if (s.isEmpty() || "null".equals(s)) return null;
        try {
            Number n = NumberFormat.getNumberInstance().parse(s);
            return new BigDecimal(n.toString());
        } catch (ParseException e) {
            return null;
        }
    }
}

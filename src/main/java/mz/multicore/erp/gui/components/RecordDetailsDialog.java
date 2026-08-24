package mz.multicore.erp.gui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Inspector canónico, só-leitura, de uma linha de tabela.
 *
 * <p>Centraliza o modal aprovado para que os módulos não recriem pares etiqueta/valor, scroll,
 * campos copiáveis ou o botão Fechar. Colunas escondidas não são expostas.</p>
 */
public final class RecordDetailsDialog {
    private RecordDetailsDialog() {}

    public static void show(JTable table) {
        if (table == null || table.getSelectedRow() < 0) return;

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.NORTH;

        int detailRow = 0;
        for (int viewColumn = 0; viewColumn < table.getColumnCount(); viewColumn++) {
            if (isHidden(table, viewColumn)) continue;

            String value = valueText(table.getValueAt(table.getSelectedRow(), viewColumn));
            gbc.gridx = 0;
            gbc.gridy = detailRow;
            gbc.weightx = 0.32;
            JLabel label = new JLabel(table.getColumnName(viewColumn));
            label.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
            label.setForeground(UIHelper.ACCENT);
            fields.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.68;
            fields.add(valueComponent(value), gbc);
            detailRow++;
        }

        new ModernFormDialog(UIHelper.mainWindow, "Detalhes do Registo", "fas-info-circle",
                "Inspeção do registo seleccionado", fields)
                .asReadOnly("Fechar")
                .showDialog();
    }

    static boolean isHidden(JTable table, int viewColumn) {
        var column = table.getColumnModel().getColumn(viewColumn);
        return column.getWidth() == 0 && column.getMaxWidth() == 0;
    }

    static String valueText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static JComponent valueComponent(String value) {
        if (value.length() > 50 || value.contains("\n")) {
            JTextArea area = new JTextArea(value);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            UIHelper.styleTextArea(area);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(360, 80));
            scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
            return scroll;
        }
        JTextField field = new JTextField(value);
        field.setEditable(false);
        UIHelper.styleTextField(field);
        return field;
    }
}

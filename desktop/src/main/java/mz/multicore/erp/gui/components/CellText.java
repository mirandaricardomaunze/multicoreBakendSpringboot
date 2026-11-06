package mz.multicore.erp.gui.components;

import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Component;
import java.math.BigDecimal;

/**
 * O texto de uma célula tal como o ecrã o mostra: {@code "1.460,00 MT"} e não {@code "1460.00"}.
 *
 * <p>O modelo da tabela guarda o valor cru, porque as acções e a ordenação precisam dele. Quem o
 * repete fora da tabela (export PDF, painel de espreitar) tem de passar por aqui, senão diverge do
 * ecrã. Ver {@code FORMATO_NUMERICO_SPEC} FN-08 e FN-09.</p>
 */
public final class CellText {

    private CellText() {
    }

    /** Célula de uma linha do modelo, pela coluna da vista. */
    public static String displayed(JTable table, int modelRow, int viewColumn) {
        Object value = table.getModel().getValueAt(modelRow, table.convertColumnIndexToModel(viewColumn));
        int viewRow = modelRow < table.getModel().getRowCount() ? table.convertRowIndexToView(modelRow) : -1;
        return rendered(table, value, Math.max(0, viewRow), viewColumn);
    }

    /** Um valor do modelo, pela coluna do modelo; colunas escondidas ficam com o formato base. */
    public static String of(JTable table, Object value, int modelColumn) {
        if (table == null || modelColumn < 0 || modelColumn >= table.getModel().getColumnCount()) {
            return plain(value);
        }
        int viewColumn = table.convertColumnIndexToView(modelColumn);
        if (viewColumn < 0) return plain(value);
        return rendered(table, value, 0, viewColumn);
    }

    /** Sem tabela: números com vírgula decimal e ponto no milhar; identificadores inteiros intactos. */
    public static String plain(Object value) {
        if (value == null) return "—";
        if (value instanceof BigDecimal decimal) return TableCellRenderers.format(decimal, 2, "");
        if (value instanceof Double || value instanceof Float) {
            return TableCellRenderers.format(new BigDecimal(value.toString()), 2, "");
        }
        return plainText(RecordDetailsDialog.valueText(value));
    }

    private static String rendered(JTable table, Object value, int viewRow, int viewColumn) {
        if (value == null) return "";
        try {
            Component cell = table.getCellRenderer(viewRow, viewColumn)
                    .getTableCellRendererComponent(table, value, false, false, viewRow, viewColumn);
            if (cell instanceof JLabel label && label.getText() != null
                    && !label.getText().equals(String.valueOf(value))) {
                return plainText(label.getText());
            }
        } catch (RuntimeException ignored) {
            // Um renderer que dependa do estado da vista não pode impedir a leitura.
        }
        return plain(value);
    }

    static String plainText(String text) {
        if (text == null) return "";
        if (!text.regionMatches(true, 0, "<html>", 0, 6)) return text.trim();
        return text.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }
}

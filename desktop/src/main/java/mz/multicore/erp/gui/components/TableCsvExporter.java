package mz.multicore.erp.gui.components;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário canónico para exportação rápida de tabelas (JTable) para formato CSV / Excel.
 */
public final class TableCsvExporter {

    private TableCsvExporter() {}

    /**
     * Converte o conteúdo visível de uma JTable numa string CSV delimitada por ponto e vírgula (;).
     */
    public static String toCsvString(JTable table) {
        if (table == null) return "";

        int colCount = table.getColumnCount();
        int rowCount = table.getRowCount();

        List<Integer> visibleCols = new ArrayList<>();
        for (int c = 0; c < colCount; c++) {
            if (table.getColumnModel().getColumn(c).getWidth() > 0) {
                visibleCols.add(c);
            }
        }

        StringBuilder sb = new StringBuilder();

        // Cabeçalhos
        for (int i = 0; i < visibleCols.size(); i++) {
            if (i > 0) sb.append(';');
            int col = visibleCols.get(i);
            sb.append(escapeCsvCell(table.getColumnName(col)));
        }
        sb.append("\r\n");

        // Linhas de dados
        for (int r = 0; r < rowCount; r++) {
            for (int i = 0; i < visibleCols.size(); i++) {
                if (i > 0) sb.append(';');
                int col = visibleCols.get(i);
                Object val = table.getValueAt(r, col);
                sb.append(escapeCsvCell(val == null ? "" : String.valueOf(val)));
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    /**
     * Escapa uma célula CSV para conformidade RFC 4180 / compatibilidade Excel.
     */
    public static String escapeCsvCell(String text) {
        if (text == null) return "";
        String s = text.trim();
        boolean needsQuotes = s.contains(";") || s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) {
            s = s.replace("\"", "\"\"");
        }
        if (needsQuotes) {
            return "\"" + s + "\"";
        }
        return s;
    }

    /**
     * Abre um seletor de ficheiros e grava a tabela em ficheiro CSV codificado em UTF-8 com BOM (compatível com Excel).
     */
    public static void exportWithDialog(Component parent, JTable table, String defaultBaseName) {
        if (table == null || table.getRowCount() == 0) {
            ToastManager.show(parent, FeedbackType.WARNING, "A tabela não contém dados para exportar.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Tabela para CSV");
        String baseName = (defaultBaseName != null && !defaultBaseName.isBlank()) ? defaultBaseName : "export_dados";
        chooser.setSelectedFile(new File(baseName + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("Ficheiro CSV (*.csv)", "csv"));

        int res = chooser.showSaveDialog(parent);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            try {
                String csv = toCsvString(table);
                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     PrintWriter pw = new PrintWriter(osw)) {
                    // Escreve UTF-8 BOM para o Excel abrir com acentos corretos automaticamente
                    fos.write(0xEF);
                    fos.write(0xBB);
                    fos.write(0xBF);
                    pw.print(csv);
                }
                ToastManager.show(parent, FeedbackType.SUCCESS, "Tabela exportada com sucesso para: " + file.getName());
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao exportar tabela: " + ex.getMessage());
            }
        }
    }
}

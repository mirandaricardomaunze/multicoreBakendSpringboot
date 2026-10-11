package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.PrintApiClient;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor universal de exportação tabular para o Multicore ERP.
 * Suporta:
 * 1. CSV padrão Moçambique/Excel (UTF-8 com BOM, delimitado por ponto e vírgula).
 * 2. Excel XML Spreadsheet 2003 nativo (.xls com tipos Number/String e cabeçalhos em negrito, sem POI).
 * 3. PDF corporativo oficial com dados da empresa via {@link TableExportAction}.
 *
 * Respeita integralmente os filtros ativos da listagem e ignora paginação visual (DRY).
 */
public final class TableDataExporter {

    private TableDataExporter() {}

    /**
     * Gera CSV em conformidade com RFC 4180 / Excel a partir de todas as linhas filtradas.
     */
    public static String toCsvString(JTable table) {
        if (table == null) return "";

        List<Integer> visibleCols = visibleColumns(table);
        List<Integer> modelRows = ClientTablePagination.filteredModelRows(table);

        StringBuilder sb = new StringBuilder();

        // Cabeçalhos
        for (int i = 0; i < visibleCols.size(); i++) {
            if (i > 0) sb.append(';');
            int col = visibleCols.get(i);
            sb.append(escapeCsvCell(table.getColumnName(col)));
        }
        sb.append("\r\n");

        // Linhas filtradas
        for (int modelRow : modelRows) {
            for (int i = 0; i < visibleCols.size(); i++) {
                if (i > 0) sb.append(';');
                int viewCol = visibleCols.get(i);
                int modelCol = table.convertColumnIndexToModel(viewCol);
                Object val = table.getModel().getValueAt(modelRow, modelCol);
                sb.append(escapeCsvCell(val == null ? "" : String.valueOf(val)));
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    /**
     * Gera Excel XML Spreadsheet 2003 nativo (.xls).
     * Abre perfeitamente no Microsoft Excel, LibreOffice e Calc com cabeçalhos em negrito e colunas ajustadas.
     */
    public static String toExcelXml(JTable table, String sheetTitle) {
        if (table == null) return "";

        List<Integer> visibleCols = visibleColumns(table);
        List<Integer> modelRows = ClientTablePagination.filteredModelRows(table);
        String title = (sheetTitle != null && !sheetTitle.isBlank()) ? sheetTitle : "Dados";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\r\n");
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\r\n");
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\r\n");
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\r\n");
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\r\n");

        // Estilos
        sb.append(" <Styles>\r\n");
        sb.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\r\n");
        sb.append("   <Alignment ss:Vertical=\"Center\"/>\r\n");
        sb.append("   <Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Color=\"#1E293B\"/>\r\n");
        sb.append("  </Style>\r\n");
        sb.append("  <Style ss:ID=\"Header\">\r\n");
        sb.append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\r\n");
        sb.append("   <Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\r\n");
        sb.append("   <Interior ss:Color=\"#1E3A8A\" ss:Pattern=\"Solid\"/>\r\n");
        sb.append("  </Style>\r\n");
        sb.append("  <Style ss:ID=\"Number\">\r\n");
        sb.append("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\r\n");
        sb.append("   <NumberFormat ss:Format=\"#,##0.00\"/>\r\n");
        sb.append("  </Style>\r\n");
        sb.append(" </Styles>\r\n");

        // Aba
        sb.append(" <Worksheet ss:Name=\"").append(escapeXml(title)).append("\">\r\n");
        sb.append("  <Table>\r\n");

        // Definição de largura de colunas
        for (int i = 0; i < visibleCols.size(); i++) {
            sb.append("   <Column ss:Width=\"130\"/>\r\n");
        }

        // Cabeçalho
        sb.append("   <Row ss:Height=\"24\">\r\n");
        for (int viewCol : visibleCols) {
            String colName = table.getColumnName(viewCol);
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">")
                    .append(escapeXml(colName))
                    .append("</Data></Cell>\r\n");
        }
        sb.append("   </Row>\r\n");

        // Linhas de dados
        for (int modelRow : modelRows) {
            sb.append("   <Row ss:Height=\"20\">\r\n");
            for (int viewCol : visibleCols) {
                int modelCol = table.convertColumnIndexToModel(viewCol);
                Object val = table.getModel().getValueAt(modelRow, modelCol);
                appendXmlCell(sb, val);
            }
            sb.append("   </Row>\r\n");
        }

        sb.append("  </Table>\r\n");
        sb.append(" </Worksheet>\r\n");
        sb.append("</Workbook>\r\n");

        return sb.toString();
    }

    private static void appendXmlCell(StringBuilder sb, Object val) {
        if (val == null) {
            sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\r\n");
            return;
        }

        if (val instanceof BigDecimal bd) {
            sb.append("    <Cell ss:StyleID=\"Number\"><Data ss:Type=\"Number\">")
                    .append(bd.toPlainString())
                    .append("</Data></Cell>\r\n");
            return;
        }

        if (val instanceof Number num) {
            sb.append("    <Cell ss:StyleID=\"Number\"><Data ss:Type=\"Number\">")
                    .append(num.toString())
                    .append("</Data></Cell>\r\n");
            return;
        }

        String str = String.valueOf(val).trim();
        // Tenta detectar valores monetários do tipo "1.250,00 MT"
        if (str.endsWith(" MT") || str.endsWith(" MZN")) {
            try {
                String clean = str.replaceAll("(?i)\\s*(MT|MZN)\\s*$", "")
                        .replace(" ", "").replace("\u00A0", "").replace(',', '.');
                new BigDecimal(clean); // valida se é número
                sb.append("    <Cell ss:StyleID=\"Number\"><Data ss:Type=\"Number\">")
                        .append(clean)
                        .append("</Data></Cell>\r\n");
                return;
            } catch (Exception ignored) {}
        }

        sb.append("    <Cell><Data ss:Type=\"String\">")
                .append(escapeXml(str))
                .append("</Data></Cell>\r\n");
    }

    public static void exportToCsvWithDialog(Component parent, JTable table, String defaultBaseName) {
        if (table == null || ClientTablePagination.filteredModelRows(table).isEmpty()) {
            ToastManager.show(parent, FeedbackType.WARNING, "A tabela não contém dados para exportar.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar para CSV (Compatível com Excel)");
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
                    // Escreve UTF-8 BOM
                    fos.write(0xEF);
                    fos.write(0xBB);
                    fos.write(0xBF);
                    pw.print(csv);
                }
                ToastManager.show(parent, FeedbackType.SUCCESS, "Ficheiro CSV guardado com sucesso: " + file.getName());
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gravar CSV: " + ex.getMessage());
            }
        }
    }

    public static void exportToExcelWithDialog(Component parent, JTable table, String defaultBaseName) {
        if (table == null || ClientTablePagination.filteredModelRows(table).isEmpty()) {
            ToastManager.show(parent, FeedbackType.WARNING, "A tabela não contém dados para exportar.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar para Microsoft Excel");
        String baseName = (defaultBaseName != null && !defaultBaseName.isBlank()) ? defaultBaseName : "export_dados";
        chooser.setSelectedFile(new File(baseName + ".xls"));
        chooser.setFileFilter(new FileNameExtensionFilter("Ficheiro Microsoft Excel (*.xls)", "xls"));

        int res = chooser.showSaveDialog(parent);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".xls")) {
                file = new File(file.getParentFile(), file.getName() + ".xls");
            }
            try {
                String xml = toExcelXml(table, baseName);
                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     PrintWriter pw = new PrintWriter(osw)) {
                    pw.print(xml);
                }
                ToastManager.show(parent, FeedbackType.SUCCESS, "Folha de cálculo Excel guardada com sucesso: " + file.getName());
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gravar folha Excel: " + ex.getMessage());
            }
        }
    }

    public static void exportToPdf(Component parent, PrintApiClient printApiClient, JTable table, String title, String defaultBaseName) {
        TableExportAction.export(parent, printApiClient, table, title, defaultBaseName);
    }

    private static List<Integer> visibleColumns(JTable table) {
        List<Integer> list = new ArrayList<>();
        int count = table.getColumnCount();
        for (int i = 0; i < count; i++) {
            if (table.getColumnModel().getColumn(i).getWidth() > 0) {
                list.add(i);
            }
        }
        return list;
    }

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

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

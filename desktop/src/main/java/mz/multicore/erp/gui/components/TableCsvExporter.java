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

    public static String toCsvString(JTable table) {
        return TableDataExporter.toCsvString(table);
    }

    public static String escapeCsvCell(String text) {
        return TableDataExporter.escapeCsvCell(text);
    }

    public static void exportWithDialog(Component parent, JTable table, String defaultBaseName) {
        TableDataExporter.exportToCsvWithDialog(parent, table, defaultBaseName);
    }
}

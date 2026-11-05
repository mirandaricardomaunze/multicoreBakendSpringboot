package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.modules.printing.dto.TableExportRequest;

import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * "Exportar PDF" de uma listagem — um só sítio, para todos os ecrãs.
 *
 * <p>O desktop chegou a desenhar estas exportações sozinho e saíam folhas <b>sem os dados da
 * empresa</b>: sem nome, sem NUIT, sem morada. Papel que não diz quem o emitiu, contra o que a
 * {@code DADOS_EMPRESA_DOCUMENTOS_SPEC} exige de todos os documentos imprimíveis. A empresa vive
 * no servidor — o desktop é um cliente fino — por isso a listagem sobe e desce PDF, com o mesmo
 * cabeçalho da factura e da guia.</p>
 *
 * <p>Leva <b>o que o operador filtrou</b>, todas as páginas: a paginação é para ler, não define o
 * que se exporta. Ver {@link ClientTablePagination#filteredModelRows(JTable)}.</p>
 */
public final class TableExportAction {

    private TableExportAction() {
    }

    /**
     * Exporta a listagem visível e abre o modal de impressão.
     *
     * @param owner    componente sobre o qual o progresso e o modal aparecem
     * @param table    tabela a exportar (filtros respeitados, paginação ignorada)
     * @param title    título do relatório, como sai no PDF (ex.: {@code "Clientes"})
     * @param baseName nome-base do ficheiro (ex.: {@code "clientes"}); leva o sufixo {@code -export}
     */
    public static void export(Component owner, PrintApiClient printApiClient, JTable table,
                              String title, String baseName) {
        if (printApiClient == null) {
            ToastManager.show(owner, FeedbackType.ERROR,
                    "Exportação indisponível: o cliente de impressão não está ligado.");
            return;
        }
        List<Integer> rows = ClientTablePagination.filteredModelRows(table);
        if (rows.isEmpty()) {
            ToastManager.show(owner, FeedbackType.WARNING, "Não existem dados na vista actual para exportar.");
            return;
        }
        TableExportRequest request = snapshot(title, table, rows);
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        UIHelper.runWithProgress(owner, "A gerar o PDF da listagem…",
                () -> printApiClient.renderTable(companyId, request),
                pdf -> PrintPreviewDialog.show(owner, pdf, baseName + "-export", title),
                error -> ToastManager.show(owner, FeedbackType.ERROR,
                        "Não foi possível exportar: " + error.getMessage()));
    }

    /**
     * O que está no ecrã, em texto. As células vão já formatadas — é o que o operador leu, e é o
     * que ele espera reencontrar no papel.
     */
    static TableExportRequest snapshot(String title, JTable table, List<Integer> modelRows) {
        int columns = table.getColumnCount();
        List<String> headers = new ArrayList<>(columns);
        for (int column = 0; column < columns; column++) {
            headers.add(table.getColumnName(column));
        }
        List<List<String>> rows = new ArrayList<>(modelRows.size());
        for (int row : modelRows) {
            List<String> cells = new ArrayList<>(columns);
            for (int column = 0; column < columns; column++) {
                cells.add(displayedText(table, row, column));
            }
            rows.add(cells);
        }
        return new TableExportRequest(title, headers, rows);
    }

    /**
     * O texto que o renderer da coluna mostra: "1.460,00 MT" e não "1460.00". Sem isto o PDF divergia
     * do ecrã em todos os valores e quantidades. Se o renderer não for um rótulo, vai o valor cru.
     */
    static String displayedText(JTable table, int modelRow, int viewColumn) {
        Object value = table.getModel().getValueAt(modelRow, table.convertColumnIndexToModel(viewColumn));
        if (value == null) return "";
        try {
            int viewRow = Math.max(0, table.convertRowIndexToView(modelRow));
            Component cell = table.getCellRenderer(viewRow, viewColumn)
                    .getTableCellRendererComponent(table, value, false, false, viewRow, viewColumn);
            if (cell instanceof JLabel label && label.getText() != null) return plainText(label.getText());
        } catch (RuntimeException ignored) {
            // Um renderer que dependa do estado da vista não pode impedir a exportação.
        }
        return String.valueOf(value);
    }

    private static String plainText(String text) {
        if (!text.regionMatches(true, 0, "<html>", 0, 6)) return text.trim();
        return text.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }
}

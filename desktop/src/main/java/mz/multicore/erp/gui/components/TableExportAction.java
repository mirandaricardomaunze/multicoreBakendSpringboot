package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.modules.printing.dto.TableExportRequest;

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
                Object value = table.getModel().getValueAt(row, table.convertColumnIndexToModel(column));
                cells.add(value == null ? "" : String.valueOf(value));
            }
            rows.add(cells);
        }
        return new TableExportRequest(title, headers, rows);
    }
}

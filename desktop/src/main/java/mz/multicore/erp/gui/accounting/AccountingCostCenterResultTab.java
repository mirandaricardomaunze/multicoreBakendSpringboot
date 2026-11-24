package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultDTO;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultLineDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/**
 * Aba "Resultados por centro" (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.3): proveitos, custos e resultado de cada
 * centro de custo no período, mais a linha Total, igual à demonstração de resultados. Só mostra o que o backend
 * calcula; exporta pelos meios normais de tabela (PDF, Excel e CSV no menu de contexto).
 */
public final class AccountingCostCenterResultTab extends JPanel {

    private static final String NO_CENTER = "Sem centro";

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final DefaultTableModel resultModel = AccountingUiHelper.createTableModel(
            "Código", "Centro de custo", "Proveitos", "Custos", "Resultado");
    private final JTable table = AccountingUiHelper.styledTable(resultModel, true);
    private final JLabel summary = AccountingUiHelper.createMutedLabel();
    private final DateField from = new DateField();
    private final DateField to = new DateField();

    public AccountingCostCenterResultTab(AccountingApiClient accountingApiClient, PrintApiClient printApiClient,
                                         InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        LocalDate today = LocalDate.now();
        from.setText(today.withDayOfMonth(1).toString());
        to.setText(today.toString());

        table.setAutoCreateRowSorter(false); // a linha Total fica sempre no fim
        AccountingUiHelper.applyMoneyRenderers(table, 2, 3, 4);
        ModernPanel card = AccountingUiHelper.createTableCard(table);

        ModernButton load = UIHelper.createPrimaryButton("Calcular");
        load.setIcon(UIHelper.icon("fas-calculator", 14));
        load.addActionListener(e -> loadResult());
        ModernButton pdf = UIHelper.createSecondaryButton("PDF");
        pdf.setIcon(UIHelper.icon("fas-file-pdf", 14));
        pdf.setToolTipText("Exportar a listagem em PDF com os dados da empresa");
        pdf.addActionListener(e -> TableExportAction.export(this, printApiClient, table,
                "Resultados por centro de custo", "resultados-por-centro"));
        ModernButton excel = UIHelper.createExportButton(table, "resultados-por-centro");

        JPanel filters = TableFilter.bar(TableFilter.label("De:", "fas-calendar-alt"), from,
                TableFilter.label("A:", "fas-calendar-alt"), to);
        card.add(UIHelper.tableCardTop("Resultados por centro de custo", filters, load, pdf, excel), BorderLayout.NORTH);
        card.add(summary, BorderLayout.SOUTH);

        add(AccountingUiHelper.wrapTabCard(card), BorderLayout.CENTER);
    }

    public void loadResult() {
        LocalDate start = AccountingUiHelper.parseDate(from.getText());
        LocalDate end = AccountingUiHelper.parseDate(to.getText());
        UIHelper.loadAsync(this, () -> accountingApiClient.getCostCenterResult(start, end), this::applyResult,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível calcular os resultados por centro",
                        error.getMessage(), null, null));
    }

    /** Mostra o relatório calculado pelo backend: uma linha por centro e a linha Total. */
    void applyResult(CostCenterResultDTO result) {
        resultModel.setRowCount(0);
        for (CostCenterResultLineDTO line : result.centers()) {
            boolean noCenter = line.costCenterId() == null;
            resultModel.addRow(new Object[]{noCenter ? "" : line.costCenterCode(),
                    noCenter ? NO_CENTER : line.costCenterName(), line.revenue(), line.expense(), line.result()});
        }
        resultModel.addRow(new Object[]{"", "Total", result.totalRevenue(), result.totalExpense(), result.netResult()});
        summary.setText(String.format("<html><b>Período:</b> %s a %s &nbsp;·&nbsp; <b>Resultado:</b> %s</html>",
                result.from().format(AccountingUiHelper.DATE_FMT), result.to().format(AccountingUiHelper.DATE_FMT),
                UIHelper.formatMzn(result.netResult())));
    }

    JTable resultTable() {
        return table;
    }

    JLabel summaryLabel() {
        return summary;
    }
}

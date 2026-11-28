package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceLineDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceQuery;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.model.BudgetVarianceDirection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Aba "Real vs Orçado" (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.4, OR-UI-02): por conta × centro, o orçado, o real, o
 * desvio e o desvio % do mês ou acumulado, com o sentido favorável/desfavorável colorido pelo {@link StatusTone}.
 * Cliente-fino: o backend calcula tudo; aqui só se escolhe o período e se mostra.
 */
public final class AccountingBudgetVarianceTab extends JPanel {

    static final int VARIANCE_COLUMN = 4;
    static final int VARIANCE_PERCENT_COLUMN = 5;
    static final int DIRECTION_COLUMN = 6;

    private static final String[] MONTHS = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho",
            "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
    private static final String ONLY_MONTH = "Só o mês";
    private static final String CUMULATIVE = "Acumulado (Jan até ao mês)";

    /** Centre filter entry; {@code id == null} reports every centre. */
    record CenterOption(Long id, String label) {}

    private static final CenterOption ALL_CENTERS = new CenterOption(null, "Todos os centros");

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final DefaultTableModel model = AccountingUiHelper.createTableModel(
            "Conta", "Centro", "Orçado", "Real", "Desvio", "Desvio %", "Situação");
    private final List<BudgetVarianceDirection> directions = new ArrayList<>();
    private final JTable table = AccountingUiHelper.styledTable(model, true);
    private final JLabel summary = AccountingUiHelper.createMutedLabel();
    private final JSpinner year = UIHelper.createYearSpinner(LocalDate.now().getYear());
    private final JComboBox<Integer> month = UIHelper.createComboBox(
            new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, AccountingBudgetVarianceTab::monthName);
    private final JComboBox<String> period = UIHelper.createComboBox(new String[]{ONLY_MONTH, CUMULATIVE}, null);
    private final JComboBox<CenterOption> center = UIHelper.createComboBox(new CenterOption[]{ALL_CENTERS},
            CenterOption::label);
    private final ModernButton calculate = UIHelper.createPrimaryButton("Calcular");

    public AccountingBudgetVarianceTab(AccountingApiClient accountingApiClient, InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        month.setSelectedItem(LocalDate.now().getMonthValue());
        UIHelper.styleSpinner(year);
        AccountingUiHelper.applyMoneyRenderers(table, 2, 3);
        table.getColumnModel().getColumn(VARIANCE_COLUMN).setCellRenderer(new DirectionRenderer(" MT", true));
        table.getColumnModel().getColumn(VARIANCE_PERCENT_COLUMN).setCellRenderer(new DirectionRenderer(" %", true));
        table.getColumnModel().getColumn(DIRECTION_COLUMN).setCellRenderer(new DirectionRenderer("", false));
        ModernPanel card = AccountingUiHelper.createTableCard(table);

        calculate.setIcon(UIHelper.icon("fas-calculator", 14));
        calculate.addActionListener(e -> loadReport());
        calculate.setEnabled(SignedInUser.isManagerOrAdmin());

        JPanel filters = TableFilter.bar(TableFilter.label("Ano:", "fas-calendar-alt"), year,
                TableFilter.label("Mês:", "fas-calendar-day"), month, TableFilter.label("Período:"), period,
                TableFilter.label("Centro:", "fas-project-diagram"), center);
        card.add(UIHelper.tableCardTop("Real vs orçado (MT)", filters, calculate), BorderLayout.NORTH);
        card.add(summary, BorderLayout.SOUTH);
        add(AccountingUiHelper.wrapTabCard(card), BorderLayout.CENTER);
    }

    /** Só ADMIN/MANAGER consultam o real vs orçado (SPEC §4.1): sem perfil não há pedido nem erro a mostrar. */
    public void onPanelSelected() {
        calculate.setEnabled(SignedInUser.isManagerOrAdmin());
        if (!calculate.isEnabled()) return;
        UIHelper.loadAsync(this, accountingApiClient::listCostCenters, this::applyCenters, error -> { });
        loadReport();
    }

    public void loadReport() {
        BudgetVarianceQuery query = currentQuery();
        UIHelper.loadAsync(this, () -> accountingApiClient.getBudgetVariance(query.year(), query.month(),
                        query.cumulative(), query.costCenterId()), this::applyReport,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível calcular o real vs orçado",
                        error.getMessage(), null, null));
    }

    /** Período e centro escolhidos nos filtros. */
    BudgetVarianceQuery currentQuery() {
        CenterOption chosen = (CenterOption) center.getSelectedItem();
        return new BudgetVarianceQuery(((Number) year.getValue()).intValue(), (Integer) month.getSelectedItem(),
                CUMULATIVE.equals(period.getSelectedItem()), chosen == null ? null : chosen.id());
    }

    /** Filtro de centros: "Todos" e todos os centros (os inactivos também têm histórico, SPEC §4.2). */
    void applyCenters(List<CostCenterDTO> centers) {
        CenterOption previous = (CenterOption) center.getSelectedItem();
        center.removeAllItems();
        center.addItem(ALL_CENTERS);
        for (CostCenterDTO costCenter : centers == null ? List.<CostCenterDTO>of() : centers) {
            CenterOption option = new CenterOption(costCenter.id(), costCenter.code() + " — " + costCenter.name());
            center.addItem(option);
            if (previous != null && option.id() != null && option.id().equals(previous.id())) center.setSelectedItem(option);
        }
    }

    /** Mostra o relatório do backend: uma linha por conta × centro, desvios coloridos pelo sentido. */
    void applyReport(BudgetVarianceDTO report) {
        if (report == null) return;
        model.setRowCount(0);
        directions.clear();
        int unfavourable = 0;
        for (BudgetVarianceLineDTO line : report.lines()) {
            directions.add(line.direction());
            if (line.direction() == BudgetVarianceDirection.UNFAVORABLE) unfavourable++;
            model.addRow(new Object[]{line.accountCode() + " — " + line.accountName(),
                    line.costCenterCode() + " — " + line.costCenterName(), line.budgeted(), line.actual(),
                    line.variance(), line.variancePercent(), directionLabel(line.direction())});
        }
        String when = report.cumulative() ? "Janeiro a " + monthName(report.month()) : monthName(report.month());
        summary.setText(report.lines().isEmpty()
                ? String.format("<html><b>%s de %d:</b> sem orçamento nem real no período.</html>", when, report.year())
                : String.format("<html><b>%s de %d</b> &nbsp;·&nbsp; %d linha(s) &nbsp;·&nbsp; %d desfavorável(eis)</html>",
                        when, report.year(), report.lines().size(), unfavourable));
    }

    static String directionLabel(BudgetVarianceDirection direction) {
        if (direction == null) return "";
        return switch (direction) {
            case FAVORABLE -> "Favorável";
            case UNFAVORABLE -> "Desfavorável";
            case ON_BUDGET -> "No orçamento";
        };
    }

    private static String monthName(Integer month) {
        return month == null || month < 1 || month > 12 ? "" : MONTHS[month - 1];
    }

    /** Colours a cell with the {@link StatusTone} of its row's direction (favourable green, unfavourable red). */
    private final class DirectionRenderer extends DefaultTableCellRenderer {
        private final String suffix;
        private final boolean numeric;

        private DirectionRenderer(String suffix, boolean numeric) {
            this.suffix = suffix;
            this.numeric = numeric;
            setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable source, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            Object shown = numeric ? TableCellRenderers.format((BigDecimal) value, 2, suffix) : value;
            super.getTableCellRendererComponent(source, shown, selected, focus, row, column);
            if (!numeric) setFont(Typography.CAPTION_BOLD);
            int modelRow = source.convertRowIndexToModel(row);
            String label = modelRow < directions.size() ? directionLabel(directions.get(modelRow)) : "";
            setForeground(selected ? source.getSelectionForeground() : StatusTone.colorOf(StatusTone.toneOf(label)));
            return this;
        }
    }

    JTable varianceTable() {
        return table;
    }

    JLabel summaryLabel() {
        return summary;
    }

    JSpinner yearSpinner() {
        return year;
    }

    JComboBox<Integer> monthCombo() {
        return month;
    }

    JComboBox<String> periodCombo() {
        return period;
    }

    JComboBox<CenterOption> centerCombo() {
        return center;
    }

    ModernButton calculateButton() {
        return calculate;
    }
}

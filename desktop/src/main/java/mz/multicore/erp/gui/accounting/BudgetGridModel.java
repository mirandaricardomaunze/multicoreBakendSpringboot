package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.modules.accounting.dto.BudgetDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetLineDTO;
import mz.multicore.erp.modules.accounting.dto.SaveBudgetRequest;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Annual budget grid (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.4, OR-UI-01): one row per account × cost centre with
 * the twelve months and a read-only row total. An empty cell ({@code null}) has no budget and is not sent; zero is a
 * filled cell. Only shapes the request — the budget rules live in the backend.
 */
public final class BudgetGridModel extends AbstractTableModel {

    static final int FIRST_MONTH_COLUMN = 2;
    static final int TOTAL_COLUMN = FIRST_MONTH_COLUMN + 12;
    private static final String[] COLUMNS = {"Conta", "Centro", "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul",
            "Ago", "Set", "Out", "Nov", "Dez", "Total"};

    private final List<Row> rows = new ArrayList<>();
    private boolean editable = true;

    private static final class Row {
        private final String accountCode;
        private final String accountLabel;
        private final Long costCenterId;
        private final String costCenterLabel;
        private final BigDecimal[] months = new BigDecimal[12];

        private Row(String accountCode, String accountName, Long costCenterId, String costCenterLabel) {
            this.accountCode = accountCode;
            this.accountLabel = accountName == null || accountName.isBlank() ? accountCode : accountCode + " — " + accountName;
            this.costCenterId = costCenterId;
            this.costCenterLabel = costCenterLabel;
        }

        private BigDecimal total() {
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal amount : months) {
                if (amount != null) total = total.add(amount);
            }
            return total;
        }
    }

    /** Shows the budget returned by the backend, grouping its cells by account × centre (account code order). */
    public void applyBudget(BudgetDTO budget) {
        Map<String, Row> byKey = new LinkedHashMap<>();
        for (BudgetLineDTO line : budget == null ? List.<BudgetLineDTO>of() : budget.lines()) {
            Row row = byKey.computeIfAbsent(line.accountCode() + "#" + line.costCenterId(), key -> new Row(
                    line.accountCode(), line.accountName(), line.costCenterId(),
                    line.costCenterCode() + " — " + line.costCenterName()));
            if (line.month() >= 1 && line.month() <= 12) row.months[line.month() - 1] = line.amount();
        }
        rows.clear();
        rows.addAll(byKey.values());
        rows.sort(Comparator.comparing(row -> row.accountCode));
        fireTableDataChanged();
    }

    /** Adds an empty account × centre row; the same pair twice is refused. */
    public void addLine(String accountCode, String accountName, Long costCenterId, String costCenterLabel) {
        boolean exists = rows.stream().anyMatch(row -> row.accountCode.equals(accountCode)
                && Objects.equals(row.costCenterId, costCenterId));
        if (exists) throw new IllegalArgumentException("A conta " + accountCode + " com o centro " + costCenterLabel
                + " já está no orçamento.");
        rows.add(new Row(accountCode, accountName, costCenterId, costCenterLabel));
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    /** Removes the row; its cells leave the budget on the next save. */
    public void removeLine(int row) {
        rows.remove(row);
        fireTableRowsDeleted(row, row);
    }

    /** The whole year as the backend expects it: only the filled cells (zero included). */
    public SaveBudgetRequest toRequest(int year) {
        List<SaveBudgetRequest.Line> lines = new ArrayList<>();
        for (Row row : rows) {
            for (int month = 1; month <= 12; month++) {
                BigDecimal amount = row.months[month - 1];
                if (amount != null) lines.add(new SaveBudgetRequest.Line(row.accountCode, row.costCenterId, month, amount));
            }
        }
        return new SaveBudgetRequest(year, lines);
    }

    /** Typed amount: blank = empty cell; negative or non-numeric text is refused with a PT-MZ message. */
    public static BigDecimal parseAmount(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return requireNonNegative(decimal);
        String text = value.toString().trim();
        if (text.isEmpty()) return null;
        try {
            return requireNonNegative(new BigDecimal(MoneyField.normalize(text)));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido: \"" + text + "\". Use apenas números (ex.: 1500,00).");
        }
    }

    private static BigDecimal requireNonNegative(BigDecimal amount) {
        if (amount.signum() < 0) throw new IllegalArgumentException("O valor orçado não pode ser negativo.");
        return amount;
    }

    /** Without ADMIN/MANAGER the grid is read-only (the backend decides anyway). */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column < FIRST_MONTH_COLUMN ? String.class : BigDecimal.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return editable && column >= FIRST_MONTH_COLUMN && column < TOTAL_COLUMN;
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        Row row = rows.get(rowIndex);
        if (column == 0) return row.accountLabel;
        if (column == 1) return row.costCenterLabel;
        if (column == TOTAL_COLUMN) return row.total();
        return row.months[column - FIRST_MONTH_COLUMN];
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int column) {
        if (column < FIRST_MONTH_COLUMN || column >= TOTAL_COLUMN) return;
        rows.get(rowIndex).months[column - FIRST_MONTH_COLUMN] = parseAmount(value);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }
}

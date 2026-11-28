package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.BudgetDTO;
import mz.multicore.erp.modules.accounting.dto.CopyBudgetRequest;
import mz.multicore.erp.modules.accounting.dto.SaveBudgetRequest;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Year;

/**
 * Aba "Orçamento" (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.4, OR-UI-01): grelha anual conta × centro × 12 meses,
 * copiar do ano anterior e gravar o ano inteiro. Cliente-fino: as regras do orçamento vivem no backend.
 */
public final class AccountingBudgetTab extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final BudgetGridModel grid = new BudgetGridModel();
    private final JTable table = AccountingUiHelper.styledTable(grid, true);
    private final JLabel summary = AccountingUiHelper.createMutedLabel();
    private final JSpinner year = UIHelper.createYearSpinner(Year.now().getValue());
    private final ModernButton add = UIHelper.createSecondaryButton("Adicionar linha");
    private final ModernButton remove = UIHelper.createSecondaryButton("Remover linha");
    private final ModernButton copy = UIHelper.createSecondaryButton("Copiar do ano anterior");
    private final ModernButton save = UIHelper.createPrimaryButton("Gravar");

    public AccountingBudgetTab(AccountingApiClient accountingApiClient, InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        table.setDefaultEditor(BigDecimal.class, new AmountCellEditor());
        for (int column = BudgetGridModel.FIRST_MONTH_COLUMN; column <= BudgetGridModel.TOTAL_COLUMN; column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.quantity());
        }
        UIHelper.styleSpinner(year);
        ModernPanel card = AccountingUiHelper.createTableCard(table);

        ModernButton load = UIHelper.createRefreshButton(this::loadBudget);
        load.setText("Carregar");
        add.setIcon(UIHelper.icon("fas-plus", 14));
        add.addActionListener(e -> BudgetLineDialog.open(accountingApiClient, grid));
        remove.setIcon(UIHelper.icon("fas-trash-alt", 14));
        remove.addActionListener(e -> removeSelected());
        copy.setIcon(UIHelper.icon("fas-copy", 14));
        copy.addActionListener(e -> copyFromPreviousYear());
        save.setIcon(UIHelper.icon("fas-save", 14));
        save.addActionListener(e -> saveBudget());
        table.getSelectionModel().addListSelectionListener(e -> refreshActions());

        JPanel filters = TableFilter.bar(TableFilter.label("Ano:", "fas-calendar-alt"), year);
        card.add(UIHelper.tableCardTop("Orçamento anual (MT)", filters, load, add, remove, copy, save),
                BorderLayout.NORTH);
        card.add(summary, BorderLayout.SOUTH);
        add(AccountingUiHelper.wrapTabCard(card), BorderLayout.CENTER);
        refreshActions();
    }

    /** Só ADMIN/MANAGER consultam o orçamento (SPEC §4.1): sem perfil não há pedido nem erro a mostrar. */
    public void onPanelSelected() {
        if (SignedInUser.isManagerOrAdmin()) loadBudget();
    }

    public void loadBudget() {
        int chosen = selectedYear();
        UIHelper.loadAsync(this, () -> accountingApiClient.getBudget(chosen), this::applyBudget,
                error -> showError("Não foi possível carregar o orçamento", error));
    }

    /** Mostra o orçamento devolvido pelo backend e os totais gravados. */
    void applyBudget(BudgetDTO budget) {
        if (budget == null) return;
        grid.applyBudget(budget);
        summary.setText(String.format("<html><b>Orçamento %d gravado</b> &nbsp;·&nbsp; Custos: %s &nbsp;·&nbsp; "
                        + "Proveitos: %s</html>", budget.year(), UIHelper.formatMzn(budget.totalExpenses()),
                UIHelper.formatMzn(budget.totalRevenues())));
        refreshActions();
    }

    private void saveBudget() {
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) return;
        SaveBudgetRequest request = grid.toRequest(selectedYear());
        UIHelper.submitAsync(save, () -> accountingApiClient.saveBudget(request), budget -> {
            applyBudget(budget);
            feedback.show(FeedbackType.SUCCESS, "Orçamento " + budget.year() + " gravado.");
        }, error -> showError("Não foi possível gravar o orçamento", error));
    }

    private void copyFromPreviousYear() {
        int target = selectedYear();
        BigDecimal adjustment = UIHelper.promptAmount("Copiar do ano anterior", "fas-copy",
                "Real de " + (target - 1) + " nas células vazias de " + target + " (0 copia o real tal como está)",
                "Ajuste (%):", BigDecimal.valueOf(-100));
        if (adjustment == null || !UIHelper.confirm(this, "O orçamento de " + target + " é gravado já com as células "
                + "copiadas e as alterações por gravar perdem-se. Continuar?", "Copiar do ano anterior")) return;
        CopyBudgetRequest request = new CopyBudgetRequest(target, adjustment);
        UIHelper.submitAsync(copy, () -> accountingApiClient.copyBudgetFromPreviousYear(request), budget -> {
            applyBudget(budget);
            feedback.show(FeedbackType.SUCCESS, "Células vazias de " + target + " preenchidas com o real de "
                    + (target - 1) + ".");
        }, error -> showError("Não foi possível copiar do ano anterior", error));
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        grid.removeLine(table.convertRowIndexToModel(row));
    }

    private void refreshActions() {
        boolean canManage = SignedInUser.isManagerOrAdmin();
        grid.setEditable(canManage);
        add.setEnabled(canManage);
        copy.setEnabled(canManage);
        save.setEnabled(canManage);
        remove.setEnabled(canManage && table.getSelectedRow() >= 0);
    }

    private int selectedYear() {
        return ((Number) year.getValue()).intValue();
    }

    private void showError(String heading, Throwable error) {
        feedback.show(FeedbackType.ERROR, heading, error.getMessage(), null, null);
    }

    /** Month cells: refuses negative or non-numeric text in the cell itself, with the PT-MZ message. */
    private final class AmountCellEditor extends DefaultCellEditor {
        private AmountCellEditor() {
            super(new JTextField());
            UIHelper.styleTextField((JTextField) getComponent());
        }

        @Override
        public boolean stopCellEditing() {
            try {
                BudgetGridModel.parseAmount(super.getCellEditorValue());
            } catch (IllegalArgumentException ex) {
                feedback.show(FeedbackType.ERROR, ex.getMessage());
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int col) {
            String text = value instanceof BigDecimal amount ? amount.toPlainString() : "";
            return super.getTableCellEditorComponent(table, text, selected, row, col);
        }
    }

    JTable budgetTable() {
        return table;
    }

    JLabel summaryLabel() {
        return summary;
    }

    ModernButton saveButton() {
        return save;
    }

    ModernButton copyButton() {
        return copy;
    }

    ModernButton addButton() {
        return add;
    }

    ModernButton removeButton() {
        return remove;
    }
}

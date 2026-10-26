package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;

import java.util.List;

/**
 * Coordenador de apresentação, renderização e Quick Peek para notas de despesas.
 */
public final class HrExpensesViewCoordinator {

    private HrExpensesViewCoordinator() {}

    public static void populateQuickPeek(QuickPeekPanel peek, int row, HRPanel owner) {
        peek.setHeaderIcon("fas-receipt", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(owner.expensesModel.getValueAt(row, 0)));
        peek.setSubtitle(String.valueOf(owner.expensesModel.getValueAt(row, 2)));
        peek.setStatus(String.valueOf(owner.expensesModel.getValueAt(row, 3)), UIHelper.ACCENT_BLUE);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Valor Reembolsável", String.valueOf(owner.expensesModel.getValueAt(row, 1)), true),
                new QuickPeekPanel.PeekItem("Categoria", String.valueOf(owner.expensesModel.getValueAt(row, 2)), false),
                new QuickPeekPanel.PeekItem("Motivo de Rejeição", String.valueOf(owner.expensesModel.getValueAt(row, 4)), false)
        ));
    }

    public static void applyExpenses(List<ExpenseClaimDTO> loaded, HRPanel owner) {
        owner.expensesList = loaded;
        owner.expensesModel.setRowCount(0);
        for (ExpenseClaimDTO c : owner.expensesList) {
            owner.expensesModel.addRow(new Object[]{
                    c.employeeName(),
                    c.amount(),
                    c.category(),
                    c.status().name(),
                    c.rejectionReason() == null ? "" : c.rejectionReason()
            });
        }
        UIHelper.ensureHeadersFit(owner.expensesTable);
        owner.refreshOverview();
    }
}

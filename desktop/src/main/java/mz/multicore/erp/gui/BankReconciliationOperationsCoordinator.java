package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;

import javax.swing.*;
import java.math.BigDecimal;

/**
 * Coordenador das operações centrais de reconciliação:
 * auto-match, anulação de vínculos, fecho de extracto e exportação PDF.
 */
final class BankReconciliationOperationsCoordinator {

    private BankReconciliationOperationsCoordinator() {}

    static void performAutoMatch(BankReconciliationPanel owner) {
        BankStatementDTO st = (BankStatementDTO) owner.statementCombo.getSelectedItem();
        if (st == null) {
            owner.showNotice(FeedbackType.WARNING, "Selecção necessária", "Seleccione um extracto para auto-conciliar.");
            return;
        }

        UIHelper.loadAsync(owner,
                () -> owner.apiClient.autoMatch(st.id()),
                res -> {
                    Object count = res != null ? res.get("matchedCount") : 0;
                    ToastManager.success(owner, "Auto-conciliação concluída: " + count + " movimentos associados.");
                    owner.onStatementChanged();
                },
                error -> owner.showNotice(FeedbackType.ERROR, "Erro ao auto-conciliar", error.getMessage()));
    }

    static void emitPdfReport(BankReconciliationPanel owner) {
        BankStatementDTO st = (BankStatementDTO) owner.statementCombo.getSelectedItem();
        if (st == null) {
            owner.showNotice(FeedbackType.WARNING, "Selecção necessária", "Seleccione um extracto para emitir relatório.");
            return;
        }

        UIHelper.loadAsync(owner,
                () -> owner.apiClient.downloadReportPdf(st.id()),
                pdfBytes -> {
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        PrintPreviewDialog.show(owner, pdfBytes, "conciliacao-bancaria-" + st.statementReference(), "Relatório de Reconciliação Bancária");
                    } else {
                        ToastManager.show(owner, FeedbackType.WARNING, "O servidor não gerou o ficheiro PDF.");
                    }
                },
                error -> owner.showNotice(FeedbackType.ERROR, "Erro ao gerar relatório", error.getMessage()));
    }

    static void closeStatement(BankReconciliationPanel owner) {
        BankStatementDTO st = (BankStatementDTO) owner.statementCombo.getSelectedItem();
        if (st == null) return;

        boolean confirmed = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(owner),
                FeedbackType.WARNING,
                "Fechar Reconciliação",
                "Tem a certeza que deseja fechar este extracto (" + st.statementReference() + ")?\n"
                        + "Uma vez fechado, as vinculações não poderão ser alteradas.",
                "Fechar Extracto");
        if (!confirmed) return;

        UIHelper.loadAsync(owner,
                () -> owner.apiClient.closeStatement(st.id()),
                closed -> {
                    ToastManager.success(owner, "Extracto bancário fechado e reconciliado com sucesso.");
                    owner.onAccountChanged();
                },
                error -> owner.showNotice(FeedbackType.ERROR, "Não foi possível fechar extracto", error.getMessage()));
    }

    static void unmatchSelectedItem(BankReconciliationPanel owner) {
        int row = owner.itemsTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "Seleccione um movimento bancário reconciliado para anular a conciliação.");
            return;
        }
        int modelRow = owner.itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = owner.currentItems.get(modelRow);

        UIHelper.loadAsync(owner,
                () -> owner.apiClient.unmatch(item.id()),
                unmatched -> {
                    ToastManager.success(owner, "Conciliação desfeita com sucesso.");
                    owner.onStatementChanged();
                },
                error -> owner.showNotice(FeedbackType.ERROR, "Erro ao desfazer conciliação", error.getMessage()));
    }

    static void updateRowActionButtons(BankReconciliationPanel owner) {
        int row = owner.itemsTable.getSelectedRow();
        if (row < 0 || row >= owner.currentItems.size()) {
            owner.setRowButtonsEnabled(false, false, false);
            return;
        }

        int modelRow = owner.itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = owner.currentItems.get(modelRow);
        BankStatementDTO st = (BankStatementDTO) owner.statementCombo.getSelectedItem();
        boolean isClosed = st != null && st.status() == BankReconciliationStatus.CLOSED;

        if (isClosed) {
            owner.setRowButtonsEnabled(false, false, false);
            return;
        }

        if (item.status() == BankStatementItemStatus.UNMATCHED) {
            owner.setRowButtonsEnabled(true, item.amount().compareTo(BigDecimal.ZERO) < 0, false);
        } else if (item.status() == BankStatementItemStatus.MATCHED) {
            owner.setRowButtonsEnabled(false, false, true);
        } else {
            owner.setRowButtonsEnabled(false, false, false);
        }

        if (owner.operationsMenu != null) {
            owner.operationsMenu.setActionEnabled(0, !isClosed);
            owner.operationsMenu.setActionEnabled(1, owner.manualMatchBtn.isEnabled());
            owner.operationsMenu.setActionEnabled(2, owner.expenseBtn.isEnabled());
            owner.operationsMenu.setActionEnabled(3, owner.unmatchBtn.isEnabled());
        }
    }
}

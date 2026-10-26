package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.PayrollCostDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Coordenador de operações visuais e entregas para retenções da folha de salários. */
final class HRLiabilitiesOperationsCoordinator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private HRLiabilitiesOperationsCoordinator() {}

    static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int row) {
        peek.setHeaderIcon("fas-university", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(model.getValueAt(row, 2)));
        peek.setSubtitle("Período: " + model.getValueAt(row, 1));
        peek.setStatus(String.valueOf(model.getValueAt(row, 5)), UIHelper.ACCENT_BLUE);
        List<QuickPeekPanel.PeekItem> items = List.of(
                new QuickPeekPanel.PeekItem("Valor da Retenção", String.valueOf(model.getValueAt(row, 3)), true),
                new QuickPeekPanel.PeekItem("Prazo Limite", String.valueOf(model.getValueAt(row, 4)), false),
                new QuickPeekPanel.PeekItem("Entregue Por", String.valueOf(model.getValueAt(row, 6)), false),
                new QuickPeekPanel.PeekItem("Referência de Pagamento", String.valueOf(model.getValueAt(row, 7)), false)
        );
        peek.setItems(items);
    }

    static String dueLabel(PayrollLiabilityDTO l) {
        if (l.dueDate() == null) {
            return "Prazo por configurar";
        }
        String date = l.dueDate().format(DATE_FMT);
        return l.overdue() ? date + " (em atraso)" : date;
    }

    static String costText(PayrollCostDTO cost) {
        if (cost == null) {
            return "Custo total do trabalhador: —";
        }
        return String.format("Custo total %02d/%d: %s  (ilíquido %s + INSS patronal %s) · líquido pago %s",
                cost.month(), cost.year(), money(cost.totalCost()),
                money(cost.grossPay()), money(cost.employerInss()),
                money(cost.netPay()));
    }

    static void deliver(HRPanel owner, JTable table, List<PayrollLiabilityDTO> liabilities, Runnable onDelivered) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione uma retenção", "Escolha uma retenção na tabela para continuar.");
            return;
        }
        PayrollLiabilityDTO sel = liabilities.get(row);
        if (!"POR_ENTREGAR".equals(sel.status())) {
            owner.showNotice(FeedbackType.INFO, "Retenção entregue", "Esta retenção já foi entregue.");
            return;
        }
        String reference = HrLiabilityDeliveryDialog.promptReference(owner, sel);
        if (reference == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A registar a entrega…",
                () -> owner.hrApiClient.deliverPayrollLiability(sel.id(), reference),
                ignored -> onDelivered.run(), owner::showActionError);
    }

    static String money(BigDecimal value) {
        return value == null ? "—" : UIHelper.formatMzn(value);
    }
}

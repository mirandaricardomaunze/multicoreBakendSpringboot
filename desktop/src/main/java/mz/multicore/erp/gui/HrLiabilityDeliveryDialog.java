package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;

import javax.swing.*;

/** Diálogo modal moderno para registo de entrega de retenção fiscal ao Estado. */
final class HrLiabilityDeliveryDialog {

    private HrLiabilityDeliveryDialog() {}

    static String promptReference(JComponent parent, PayrollLiabilityDTO liability) {
        JTextField referenceField = new JTextField();
        UIHelper.styleTextField(referenceField);
        String amountText = liability.amount() == null ? "—" : UIHelper.formatMzn(liability.amount());
        JLabel summary = new JLabel(String.format(
                "<html><b>Retenção:</b> %s<br><b>Período:</b> %02d/%d &nbsp;·&nbsp; <b>Valor:</b> %s</html>",
                liability.liabilityTypeLabel(), liability.month(), liability.year(), amountText));
        summary.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Resumo:", summary,
                "Referência do pagamento (opcional):", referenceField
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Entregar Retenção",
                "fas-check-double", "Confirmação de entrega de retenção ao Estado", form)
                .setConfirmButton("Entregar", "fas-check-double");

        if (!dialog.showDialog()) {
            return null;
        }
        return referenceField.getText().trim();
    }
}

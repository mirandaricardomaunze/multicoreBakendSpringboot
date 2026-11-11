package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;

import javax.swing.*;
import java.util.List;

/**
 * Asks where the net salary leaves from: a bank account (transfer) or the cash account. The ledger posts to Banco or
 * Caixa accordingly, so the treasury and the books stay in step.
 */
final class PayslipPaymentAccountDialog {

    private PayslipPaymentAccountDialog() {
    }

    /** The chosen account id, or null when the operator cancels or there is no account. */
    static Long choose(java.awt.Component owner, PayslipDTO payslip, List<TreasuryAccountDTO> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            mz.multicore.erp.gui.components.ToastManager.show(owner, mz.multicore.erp.gui.components.FeedbackType.WARNING,
                    "Registe uma conta de tesouraria antes de pagar salários.");
            return null;
        }
        JComboBox<String> combo = new JComboBox<>();
        UIHelper.styleComboBox(combo);
        int preferred = 0;
        for (int i = 0; i < accounts.size(); i++) {
            TreasuryAccountDTO a = accounts.get(i);
            combo.addItem(a.name() + (a.accountType() == TreasuryAccountType.BANK ? " (banco)" : " (caixa)"));
            if (a.accountType() == TreasuryAccountType.BANK && accounts.get(preferred).accountType() != TreasuryAccountType.BANK) {
                preferred = i;
            }
        }
        combo.setSelectedIndex(preferred);
        JPanel form = UIHelper.createDialogForm("Pagar a partir de:", combo);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Pagar Salário", "fas-money-check-alt",
                "Recibo " + payslip.payslipNumber() + " — " + payslip.employeeName() + ": líquido "
                        + payslip.netPay() + " MT", form)
                .setConfirmButton("Pagar", "fas-check-circle").showDialog();
        return confirmed && combo.getSelectedIndex() >= 0 ? accounts.get(combo.getSelectedIndex()).id() : null;
    }
}

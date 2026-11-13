package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Asks which treasury account the money enters or leaves from; the ledger follows (Caixa or Banco). */
final class TreasuryAccountChooser {

    private TreasuryAccountChooser() {
    }

    /** The chosen account id, or null when cancelled or there is no account. */
    static Long choose(Component owner, String title, String subtitle, List<TreasuryAccountDTO> accounts, boolean preferBank) {
        if (accounts == null || accounts.isEmpty()) {
            ToastManager.show(owner, FeedbackType.WARNING, "Registe uma conta de tesouraria primeiro.");
            return null;
        }
        TreasuryAccountType preferred = preferBank ? TreasuryAccountType.BANK : TreasuryAccountType.CASH;
        JComboBox<String> combo = new JComboBox<>();
        UIHelper.styleComboBox(combo);
        int selected = 0;
        for (int i = 0; i < accounts.size(); i++) {
            TreasuryAccountDTO a = accounts.get(i);
            combo.addItem(a.name() + (a.accountType() == TreasuryAccountType.BANK ? " (banco)" : " (caixa)"));
            if (a.accountType() == preferred && accounts.get(selected).accountType() != preferred) selected = i;
        }
        combo.setSelectedIndex(selected);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, title, "fas-university", subtitle,
                UIHelper.createDialogForm("Conta:", combo)).setConfirmButton("Confirmar", "fas-check-circle").showDialog();
        return confirmed && combo.getSelectedIndex() >= 0 ? accounts.get(combo.getSelectedIndex()).id() : null;
    }
}

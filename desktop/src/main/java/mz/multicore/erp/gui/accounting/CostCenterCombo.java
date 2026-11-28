package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;

import javax.swing.*;
import java.util.List;

/**
 * Cost-centre picker for manual postings and expense claims (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.2, CC-UI-02):
 * lists the active centres of the active company with the default centre pre-selected. Until the list arrives — or
 * when the user cannot read it — it shows a single "por omissão" option that sends no centre, so the backend applies
 * the company's default centre.
 */
public final class CostCenterCombo extends JComboBox<CostCenterCombo.Option> {

    /** One entry of the picker; {@code id == null} means "let the backend use the default centre". */
    public record Option(Long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final Option BACKEND_DEFAULT = new Option(null, "Centro por omissão da empresa");

    public CostCenterCombo() {
        UIHelper.styleComboBox(this);
        applyCenters(List.of());
    }

    /** Loads the centres in the background; on failure keeps the "por omissão" option. */
    public CostCenterCombo load(AccountingApiClient accountingApiClient) {
        if (accountingApiClient != null) {
            UIHelper.loadAsync(this, () -> accountingApiClient.listCostCenters(), this::applyCenters, error -> { });
        }
        return this;
    }

    /** Shows the active centres, selecting the default one. */
    public void applyCenters(List<CostCenterDTO> centers) {
        removeAllItems();
        Option selected = null;
        for (CostCenterDTO center : centers == null ? List.<CostCenterDTO>of() : centers) {
            if (!center.active()) continue;
            Option option = new Option(center.id(), center.code() + " — " + center.name());
            addItem(option);
            if (center.defaultCenter()) selected = option;
        }
        if (getItemCount() == 0) {
            addItem(BACKEND_DEFAULT);
            selected = BACKEND_DEFAULT;
        }
        setSelectedItem(selected != null ? selected : getItemAt(0));
    }

    /** Chosen centre id, or {@code null} to let the backend use the default centre. */
    public Long selectedCostCenterId() {
        Object item = getSelectedItem();
        return item instanceof Option option ? option.id() : null;
    }

    /** The centre only travels on class 6/7 accounts; classes 1–5 refuse a centre (§3.2). */
    public static Long centerForAccount(String accountCode, Long costCenterId) {
        if (accountCode == null || costCenterId == null) return null;
        String code = accountCode.trim();
        return code.startsWith("6") || code.startsWith("7") ? costCenterId : null;
    }
}

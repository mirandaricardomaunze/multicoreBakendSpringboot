package mz.multicore.erp.gui.landedcost;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.desktop.client.ExchangeRateApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.gui.currency.DocumentCurrencyField;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import mz.multicore.erp.modules.purchases.dto.LandedCostChargeRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.model.LandedCostType;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * New cost line of a CI (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1, §4.5): type, supplier, currency and rate
 * (the same field as the purchase documents: proposed rate, editable), amount without VAT and the company's VAT rate.
 * Customs VAT takes no rate. The backend validates again when the CI is previewed or confirmed.
 */
final class LandedCostChargeDialog {

    /** A VAT option of the combo; {@code id} null is "Sem IVA". */
    record VatChoice(Long id, String label) {
    }

    private static final VatChoice NO_VAT = new VatChoice(null, "Sem IVA");

    private final JComboBox<LandedCostType> typeCombo =
            UIHelper.createComboBox(LandedCostType.values(), LandedCostType::label);
    private final JComboBox<SupplierDTO> supplierCombo;
    private final JComboBox<VatChoice> vatCombo;
    private final DocumentCurrencyField currencyField;
    private final DecimalField amountField = new DecimalField("", 2, true);

    LandedCostChargeDialog(List<SupplierDTO> suppliers, List<TaxRateDTO> vatRates,
                           ExchangeRateApiClient exchangeRateApiClient) {
        supplierCombo = UIHelper.createComboBox(suppliers.toArray(SupplierDTO[]::new), SupplierDTO::toString);
        List<VatChoice> choices = new ArrayList<>();
        choices.add(NO_VAT);
        vatRates.forEach(rate -> choices.add(new VatChoice(rate.id(), rate.name())));
        vatCombo = UIHelper.createComboBox(choices.toArray(VatChoice[]::new), VatChoice::label);
        currencyField = new DocumentCurrencyField(exchangeRateApiClient);
        currencyField.reset();
        typeCombo.addActionListener(event -> typeChanged());
    }

    /** Opens the dialog; the line typed, or empty when cancelled. */
    Optional<LandedCostChargeDraft> show() {
        JPanel form = UIHelper.createDialogForm("Tipo:", typeCombo, "Fornecedor:", supplierCombo,
                "Moeda e câmbio:", currencyField,
                "", new FormField("Valor sem IVA (moeda da linha)", amountField, true, "Valor do serviço ou imposto."),
                "", new FormField("IVA do serviço", vatCombo, false, "Taxas de IVA da empresa. O IVA aduaneiro não leva taxa."));
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova linha de custo", "fas-plus",
                "Frete, seguro, direitos, despachante ou IVA aduaneiro", form);
        List<LandedCostChargeDraft> result = new ArrayList<>(1);
        dialog.setConfirmButton("Adicionar linha", "fas-plus");
        dialog.setOnSave(() -> result.add(draft()));
        return dialog.showDialog() && !result.isEmpty() ? Optional.of(result.get(0)) : Optional.empty();
    }

    /** The line as typed; {@link IllegalArgumentException} with the message to show when a field is missing. */
    LandedCostChargeDraft draft() {
        SupplierDTO supplier = (SupplierDTO) supplierCombo.getSelectedItem();
        if (supplier == null) throw new IllegalArgumentException("Escolha o fornecedor da linha de custo.");
        LandedCostType type = (LandedCostType) typeCombo.getSelectedItem();
        BigDecimal rate = currencyField.exchangeRate();
        VatChoice vat = vatCombo.isEnabled() ? (VatChoice) vatCombo.getSelectedItem() : NO_VAT;
        if (vat == null) vat = NO_VAT;
        LandedCostChargeRequest request = new LandedCostChargeRequest(type, supplier.id(), currencyField.currency(),
                rate, amountField.value(), vat.id());
        String rateLabel = !currencyField.isForeign() ? DisplayNumbers.rate(BigDecimal.ONE)
                : rate == null ? "Proposto" : DisplayNumbers.rate(rate);
        return new LandedCostChargeDraft(request, supplier.toString(), rateLabel, vat.label());
    }

    private void typeChanged() {
        boolean takesVat = typeCombo.getSelectedItem() != LandedCostType.IVA_ADUANEIRO;
        if (!takesVat) vatCombo.setSelectedItem(NO_VAT);
        vatCombo.setEnabled(takesVat);
    }

    JComboBox<LandedCostType> typeCombo() { return typeCombo; }
    JComboBox<VatChoice> vatCombo() { return vatCombo; }
    JTextField amountField() { return amountField; }
}

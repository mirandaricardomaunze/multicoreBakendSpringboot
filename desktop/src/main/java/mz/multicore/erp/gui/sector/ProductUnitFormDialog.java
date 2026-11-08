package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.UnitsApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.units.dto.ProductUnitDTO;
import mz.multicore.erp.modules.units.dto.ProductUnitsDTO;
import mz.multicore.erp.modules.units.dto.SaveProductUnitRequest;
import mz.multicore.erp.modules.units.dto.UnitOfMeasureDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Adds or edits a product unit. Between units of the same dimension (kg and g) the factor is fixed and
 * computed automatically; for packages (bag, pallet, load) the user enters how many base units fit in one.
 * The preview always shows a sentence such as "1 Palete = 40 un".
 */
final class ProductUnitFormDialog {

    private ProductUnitFormDialog() {
    }

    static void show(JComponent parent, UnitsApiClient api, ProductUnitsDTO product, List<UnitOfMeasureDTO> catalog,
                     ProductUnitDTO existing, Consumer<ProductUnitsDTO> onSaved) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UnitOfMeasureDTO base = product.baseUnit();
        Set<String> used = product.units().stream().map(ProductUnitDTO::unitCode).collect(Collectors.toSet());
        List<UnitOfMeasureDTO> options = catalog.stream()
                .filter(u -> !u.code().equals(base.code()))
                .filter(u -> existing != null ? u.code().equals(existing.unitCode()) : !used.contains(u.code()))
                .toList();
        if (options.isEmpty()) {
            ToastManager.show(parent, FeedbackType.WARNING, "Este produto já tem todas as unidades disponíveis.");
            return;
        }

        SearchableComboBox<UnitOfMeasureDTO> unitCombo = new SearchableComboBox<>("Pesquisar unidade",
                u -> u.name() + " (" + u.symbol() + ")" + (u.system() ? "" : " · da empresa"),
                u -> u.code() + " " + u.name() + " " + u.dimensionLabel());
        unitCombo.setItems(options);
        if (existing != null) unitCombo.selectFirst(u -> u.code().equals(existing.unitCode()));
        unitCombo.setEnabled(existing == null);

        DecimalField factorField = new DecimalField(existing == null ? "" : ProductUnitsPanel.plain(existing.factorToBase()),
                6, true);
        JTextField barcodeField = new JTextField(existing == null || existing.barcode() == null ? "" : existing.barcode());
        UIHelper.styleTextField(barcodeField);
        JCheckBox sellable = checkbox("Usar na venda", existing == null || existing.sellable());
        JCheckBox purchasable = checkbox("Usar na compra", existing == null || existing.purchasable());
        JCheckBox defaultSale = checkbox("Sugerir na venda", existing != null && existing.defaultSale());
        JCheckBox defaultPurchase = checkbox("Sugerir na compra", existing != null && existing.defaultPurchase());
        JLabel factorCaption = new JLabel();
        factorCaption.setForeground(UIHelper.TEXT_MUTED);
        JLabel preview = new JLabel(" ");
        preview.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        preview.setIcon(UIHelper.icon("fas-equals", 13, UIHelper.ACCENT_BLUE));
        preview.setIconTextGap(8);

        Runnable sync = () -> {
            UnitOfMeasureDTO unit = unitCombo.selectedValue();
            BigDecimal fixed = unit == null ? null : fixedFactor(unit, base);
            factorField.setEnabled(fixed == null);
            if (fixed != null) factorField.setText(ProductUnitsPanel.plain(fixed));
            String unitName = unit == null ? "unidade" : unit.name();
            factorCaption.setText(fixed != null
                    ? "Conversão fixa: não se escolhe."
                    : "Quantas " + base.name().toLowerCase() + " (" + base.symbol() + ") tem 1 " + unitName.toLowerCase() + "?");
            BigDecimal factor = fixed != null ? fixed : parse(factorField.getText());
            preview.setForeground(factor == null ? UIHelper.TEXT_MUTED : UIHelper.TEXT_LIGHT);
            preview.setText(factor == null ? "Indique o factor para ver a conversão"
                    : "1 " + unitName + " = " + ProductUnitsPanel.plain(factor) + " " + base.symbol());
            defaultSale.setEnabled(sellable.isSelected());
            if (!sellable.isSelected()) defaultSale.setSelected(false);
            defaultPurchase.setEnabled(purchasable.isSelected());
            if (!purchasable.isSelected()) defaultPurchase.setSelected(false);
        };
        unitCombo.addActionListener(e -> sync.run());
        UIHelper.onTextChange(factorField, sync);
        sellable.addActionListener(e -> sync.run());
        purchasable.addActionListener(e -> sync.run());
        sync.run();

        JPanel use = new JPanel(new GridLayout(2, 2, 12, 4));
        use.setOpaque(false);
        use.add(sellable);
        use.add(purchasable);
        use.add(defaultSale);
        use.add(defaultPurchase);

        FormField unitForm = new FormField("Unidade", unitCombo, true, null);
        FormField factorForm = new FormField("Factor de conversão", factorField, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{
                "", unitForm,
                "", factorForm,
                "", factorCaption,
                "Código de barras (opcional):", barcodeField,
                "Utilização:", use,
                "", preview});

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                existing == null ? "Adicionar unidade" : "Editar unidade", "fas-ruler-combined",
                product.productName() + " · base em " + base.name().toLowerCase(), form);
        ProductUnitsDTO[] result = new ProductUnitsDTO[1];
        dialog.setOnSaveAsync(() -> {
            UnitOfMeasureDTO unit = unitCombo.selectedValue();
            if (unit == null) {
                unitForm.setError("Escolha a unidade.");
                throw new IllegalArgumentException("Escolha a unidade.");
            }
            BigDecimal fixed = fixedFactor(unit, base);
            BigDecimal factor = fixed != null ? null : factorField.value();
            if (!sellable.isSelected() && !purchasable.isSelected()) {
                throw new IllegalArgumentException("Marque se a unidade serve para vender, para comprar, ou ambos.");
            }
            SaveProductUnitRequest request = new SaveProductUnitRequest(companyId, product.productId(), unit.code(),
                    factor, barcodeField.getText().trim(), sellable.isSelected(), purchasable.isSelected(),
                    defaultSale.isSelected(), defaultPurchase.isSelected());
            return () -> { result[0] = api.saveProductUnit(request); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onSaved.accept(result[0]);
    }

    /** Same rule as the server ({@code UnitConversionRules.fixedFactor}), used only for the preview. */
    static BigDecimal fixedFactor(UnitOfMeasureDTO unit, UnitOfMeasureDTO base) {
        if ("PACKAGE".equals(unit.dimension()) || !unit.dimension().equals(base.dimension())) return null;
        if (unit.dimensionFactor() == null || base.dimensionFactor() == null || base.dimensionFactor().signum() <= 0) return null;
        return unit.dimensionFactor().divide(base.dimensionFactor(), 6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /** Parses the number like the rest of the application ({@link MoneyField#normalize}); {@code null} if invalid. */
    static BigDecimal parse(String text) {
        try {
            BigDecimal value = new BigDecimal(MoneyField.normalize(text));
            return value.signum() > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static JCheckBox checkbox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setOpaque(false);
        box.setForeground(UIHelper.TEXT_LIGHT);
        return box;
    }
}

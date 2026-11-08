package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.UnitsApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.units.dto.ProductUnitDTO;
import mz.multicore.erp.modules.units.dto.ProductUnitsDTO;
import mz.multicore.erp.modules.units.dto.UnitConversionDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;

/**
 * "Quantidade noutra unidade" for a document line (SA-C02): the operator says "3 paletes" and the line gets
 * 120 un. The server performs the final conversion, so a fraction the base unit cannot hold is refused with
 * the same message everywhere.
 */
final class UnitQuantityDialog {

    private UnitQuantityDialog() {
    }

    static void show(JComponent parent, UnitsApiClient api, Long productId, Consumer<BigDecimal> onBaseQuantity) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || productId == null) return;
        UIHelper.loadAsync(parent, () -> api.productUnits(companyId, productId), units -> open(parent, api, companyId, units, onBaseQuantity),
                error -> ToastManager.show(parent, FeedbackType.ERROR, "Não foi possível ler as unidades: " + error.getMessage()));
    }

    private static void open(JComponent parent, UnitsApiClient api, Long companyId, ProductUnitsDTO units,
                             Consumer<BigDecimal> onBaseQuantity) {
        List<ProductUnitDTO> sellable = units.units().stream().filter(ProductUnitDTO::sellable).toList();
        if (sellable.isEmpty()) {
            ToastManager.show(parent, FeedbackType.INFO, units.productName() + " só tem a unidade base. Defina outras em "
                    + "Stock › produto › Unidades.");
            return;
        }
        String baseSymbol = units.baseUnit().symbol();
        JComboBox<ProductUnitDTO> unitCombo = new JComboBox<>(sellable.toArray(ProductUnitDTO[]::new));
        UIHelper.styleComboBox(unitCombo);
        unitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(list, value, i, s, f);
                if (value instanceof ProductUnitDTO u) {
                    setText(u.unitName() + "  (1 = " + ProductUnitsPanel.plain(u.factorToBase()) + " " + baseSymbol + ")");
                }
                return this;
            }
        });
        sellable.stream().filter(ProductUnitDTO::defaultSale).findFirst().ifPresent(unitCombo::setSelectedItem);
        DecimalField quantity = new DecimalField("1", 3, true);
        JLabel preview = new JLabel(" ");
        preview.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        preview.setIcon(UIHelper.icon("fas-equals", 13, UIHelper.ACCENT_BLUE));
        preview.setIconTextGap(8);
        Runnable sync = () -> {
            ProductUnitDTO unit = (ProductUnitDTO) unitCombo.getSelectedItem();
            BigDecimal qty = ProductUnitFormDialog.parse(quantity.getText());
            if (unit == null || qty == null) {
                preview.setForeground(UIHelper.TEXT_MUTED);
                preview.setText("Indique a quantidade");
                return;
            }
            BigDecimal base = qty.multiply(unit.factorToBase());
            boolean exact = base.setScale(units.baseUnit().decimals(), RoundingMode.HALF_UP).compareTo(base) == 0;
            preview.setForeground(exact ? UIHelper.TEXT_LIGHT : UIHelper.REJECTED_RED);
            preview.setText(ProductUnitsPanel.plain(qty) + " " + unit.unitName().toLowerCase() + " = "
                    + ProductUnitsPanel.plain(base) + " " + baseSymbol + (exact ? "" : "  (não dá unidades inteiras)"));
        };
        unitCombo.addActionListener(e -> sync.run());
        UIHelper.onTextChange(quantity, sync);
        sync.run();

        JPanel form = UIHelper.createDialogForm(1, new Object[]{"Unidade:", unitCombo, "Quantidade:", quantity, "", preview});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Quantidade noutra unidade", "fas-ruler-combined",
                units.productName() + " · a linha fica em " + units.baseUnit().name().toLowerCase(), form);
        dialog.setConfirmButton("Aplicar à linha", "fas-check");
        UnitConversionDTO[] result = new UnitConversionDTO[1];
        dialog.setOnSaveAsync(() -> {
            ProductUnitDTO unit = (ProductUnitDTO) unitCombo.getSelectedItem();
            if (unit == null) throw new IllegalArgumentException("Escolha a unidade.");
            BigDecimal qty = quantity.value();
            return () -> { result[0] = api.convert(companyId, units.productId(), unit.unitCode(), qty); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onBaseQuantity.accept(result[0].baseQuantity());
    }
}

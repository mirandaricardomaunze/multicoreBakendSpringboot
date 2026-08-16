package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.quantity.PackageQuantity;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import java.math.BigDecimal;

final class OrderPackageQuantityBinding {
    private static boolean updating;
    private OrderPackageQuantityBinding() {}

    static void packagesChanged(ComercialPanel owner) {
        if (updating || !ready(owner)) return;
        try {
            updating = true;
            int factor = selectedProduct(owner).unitsPerBox();
            long boxes = value(owner.orderBoxesField.getText());
            long loose = value(owner.orderLooseUnitsField.getText());
            owner.orderQuantityField.setText(String.valueOf(PackageQuantity.fromPackages(boxes, loose, factor).totalUnits()));
        } catch (RuntimeException ignored) {
            // Enquanto o operador escreve, o valor pode estar temporariamente incompleto.
        } finally { updating = false; }
    }

    static void totalChanged(ComercialPanel owner) {
        if (updating || !ready(owner)) return;
        try {
            updating = true;
            PackageQuantity value = PackageQuantity.fromTotal(value(owner.orderQuantityField.getText()),
                    selectedProduct(owner).unitsPerBox());
            owner.orderBoxesField.setText(String.valueOf(value.boxes()));
            owner.orderLooseUnitsField.setText(String.valueOf(value.looseUnits()));
        } catch (RuntimeException ignored) {
        } finally { updating = false; }
    }

    static String label(ProductDTO product, long total) {
        return PackageQuantity.fromTotal(total, Math.max(1, product.unitsPerBox())).label();
    }

    static void refreshDraftLogistics(ComercialPanel owner) {
        var inputs = owner.draftOrderLines.stream().map(line -> {
            ProductDTO product = owner.productsList.stream().filter(p -> p.id().equals(line.productId()))
                    .findFirst().orElse(null);
            return new LogisticsLoadCalculator.Input(line.quantity(),
                    product == null ? null : product.grossUnitWeightKg());
        }).toList();
        var shares = LogisticsLoadCalculator.calculate(inputs);
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (int index = 0; index < shares.size(); index++) {
            var share = shares.get(index);
            owner.orderLinesTableModel.setValueAt(share.lineWeightKg(), index, 2);
            owner.orderLinesTableModel.setValueAt(share.quantityPercentage() + "%", index, 3);
            owner.orderLinesTableModel.setValueAt(share.weightPercentage() + "%", index, 4);
            totalWeight = totalWeight.add(share.lineWeightKg());
        }
        owner.orderTotalLabel.setToolTipText("Peso bruto estimado da carga: " + totalWeight + " kg");
        owner.orderLoadLabel.setText("Carga: " + totalWeight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg");
    }

    private static boolean ready(ComercialPanel owner) {
        return owner.orderBoxesField != null && owner.orderLooseUnitsField != null && owner.orderQuantityField != null
                && owner.orderProductCombo != null && owner.orderProductCombo.getSelectedIndex() >= 0
                && owner.orderProductCombo.getSelectedIndex() < owner.productsList.size();
    }

    private static ProductDTO selectedProduct(ComercialPanel owner) {
        return owner.productsList.get(owner.orderProductCombo.getSelectedIndex());
    }

    private static long value(String text) {
        return text == null || text.isBlank() ? 0 : Long.parseLong(text.trim());
    }
}

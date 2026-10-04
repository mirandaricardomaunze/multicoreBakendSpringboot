package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.quantity.PackagingComposition;
import mz.multicore.erp.architecture.quantity.PackagingQuantity;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import java.math.BigDecimal;

final class OrderPackageQuantityBinding {
    private OrderPackageQuantityBinding() {}

    static String label(ProductDTO product, long total) {
        return PackagingQuantity.fromTotal(total,
                PackagingComposition.of(product.packagesPerBox(), product.unitsPerPackage())).label();
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
            owner.orderLinesTableModel.setValueAt(share.lineWeightKg(), index, 5);
            owner.orderLinesTableModel.setValueAt(share.quantityPercentage() + "%", index, 6);
            owner.orderLinesTableModel.setValueAt(share.weightPercentage() + "%", index, 7);
            totalWeight = totalWeight.add(share.lineWeightKg());
        }
        owner.orderTotalLabel.setToolTipText("Peso bruto estimado da carga: " + totalWeight + " kg");
        owner.orderLoadLabel.setText("Carga: " + totalWeight.setScale(2, java.math.RoundingMode.HALF_UP) + " kg");
    }

}

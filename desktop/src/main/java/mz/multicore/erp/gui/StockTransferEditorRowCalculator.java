package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculador de linhas e totais para o editor de transferência de stock.
 */
final class StockTransferEditorRowCalculator {

    static final int COL_PRODUCT = 0;
    static final int COL_QUANTITY = 1;
    static final int COL_PACKAGES = 2;
    static final int COL_BOXES = 3;
    static final int COL_BOX_PERCENTAGE = 4;
    static final int COL_REFERENCE = 5;
    static final int COL_BARCODE = 6;
    static final int COL_UNIT_PRICE = 7;
    static final int COL_TAX = 8;
    static final int COL_TOTAL = 9;

    private StockTransferEditorRowCalculator() {}

    static void recalculate(DefaultTableModel model, ProductSearchComboBox productCellEditor, int row, int editedColumn) {
        if (row < 0 || row >= model.getRowCount()) return;
        ProductDTO product = productCellEditor.resolve(model.getValueAt(row, COL_PRODUCT));
        int pkgsPerBox = product == null ? 1 : Math.max(1, product.packagesPerBox());
        int unitsPerPkg = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf((long) pkgsPerBox * unitsPerPkg);

        BigDecimal quantity;
        if (editedColumn == COL_PACKAGES) {
            quantity = decimal(model.getValueAt(row, COL_PACKAGES)).multiply(BigDecimal.valueOf(unitsPerPkg));
        } else if (editedColumn == COL_BOXES) {
            quantity = decimal(model.getValueAt(row, COL_BOXES)).multiply(unitsPerBox);
        } else {
            quantity = decimal(model.getValueAt(row, COL_QUANTITY));
        }

        BigDecimal packages = quantity.divide(BigDecimal.valueOf(unitsPerPkg), 2, RoundingMode.HALF_UP);
        BigDecimal boxes = quantity.divide(unitsPerBox, 2, RoundingMode.HALF_UP);
        BigDecimal percentage = quantity.multiply(BigDecimal.valueOf(100)).divide(unitsPerBox, 1, RoundingMode.HALF_UP);

        model.setValueAt(product, row, COL_PRODUCT);
        model.setValueAt(quantity, row, COL_QUANTITY);
        model.setValueAt(packages, row, COL_PACKAGES);
        model.setValueAt(boxes, row, COL_BOXES);
        model.setValueAt(percentage.stripTrailingZeros().toPlainString() + "%", row, COL_BOX_PERCENTAGE);
        model.setValueAt(product == null ? "—" : (product.reference() != null && !product.reference().isBlank() ? product.reference().trim() : (product.sku() != null ? product.sku() : "—")), row, COL_REFERENCE);
        model.setValueAt(product == null || product.barcode() == null || product.barcode().isBlank() ? "—" : product.barcode().trim(), row, COL_BARCODE);

        BigDecimal unitPrice = (product == null || product.unitPrice() == null) ? BigDecimal.ZERO : product.unitPrice();
        model.setValueAt(unitPrice, row, COL_UNIT_PRICE);
        BigDecimal tax = product == null ? BigDecimal.ZERO : product.effectiveTaxRate();
        model.setValueAt(tax.movePointRight(2).stripTrailingZeros().toPlainString() + "%", row, COL_TAX);
        model.setValueAt(unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP), row, COL_TOTAL);
    }

    static void updateTotals(DefaultTableModel model, ProductSearchComboBox productCellEditor,
                             JTextField vehicleField, JLabel totalLinesLabel, JLabel totalValueLabel) {
        BigDecimal sumQty = BigDecimal.ZERO;
        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumWeight = BigDecimal.ZERO;

        for (int i = 0; i < model.getRowCount(); i++) {
            BigDecimal qty = decimal(model.getValueAt(i, COL_QUANTITY));
            sumQty = sumQty.add(qty);
            sumTotal = sumTotal.add(decimal(model.getValueAt(i, COL_TOTAL)));
            ProductDTO p = productCellEditor.resolve(model.getValueAt(i, COL_PRODUCT));
            if (p != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal w = p.grossUnitWeightKg() != null ? p.grossUnitWeightKg() : p.netUnitWeightKg();
                if (w != null) sumWeight = sumWeight.add(w.multiply(qty));
            }
        }

        String vehicleInfo = "";
        if (sumWeight.compareTo(BigDecimal.ZERO) > 0) {
            String vehicleType = LogisticsLoadCalculator.recommendedVehicleType(sumWeight);
            BigDecimal tons = sumWeight.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            vehicleInfo = " | Carga: " + sumWeight.setScale(2, RoundingMode.HALF_UP) + " kg (" + tons.stripTrailingZeros().toPlainString() + " Ton) — Viatura Sugerida: " + vehicleType;
            if (vehicleField != null && vehicleField.getText().trim().isEmpty()) {
                vehicleField.putClientProperty("JTextField.placeholderText", "Sugerido: " + vehicleType);
            }
        }
        totalLinesLabel.setText("Itens: " + model.getRowCount() + " | Qtd Total: " + sumQty.stripTrailingZeros().toPlainString() + vehicleInfo);
        totalValueLabel.setText("Total da Transferência: " + sumTotal.setScale(2, RoundingMode.HALF_UP) + " MT");
    }

    static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal d) return d;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(value).trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

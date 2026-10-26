package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculador de linhas e totais para o editor de cotações comerciais.
 */
final class QuotationEditorRowCalculator {

    private QuotationEditorRowCalculator() {}

    record LineValues(BigDecimal net, BigDecimal tax, BigDecimal total, BigDecimal packages,
                      BigDecimal boxes, String boxPercentage) {}

    static LineValues calculate(ProductDTO product, BigDecimal quantity, BigDecimal discount) {
        if (discount == null) discount = BigDecimal.ZERO;
        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal unitPrice = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();

        BigDecimal net = unitPrice.multiply(quantity).multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
        BigDecimal tax = product == null ? BigDecimal.ZERO : product.effectiveTaxRate();
        BigDecimal total = net.multiply(BigDecimal.ONE.add(tax)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal packages = quantity.divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
        BigDecimal boxes = quantity.divide(unitsPerBox, 2, RoundingMode.HALF_UP);
        BigDecimal percentage = quantity.multiply(BigDecimal.valueOf(100)).divide(unitsPerBox, 1, RoundingMode.HALF_UP);
        return new LineValues(net, tax, total, packages, boxes, percentage.stripTrailingZeros().toPlainString() + "%");
    }

    static void updateRow(DefaultTableModel model, int row, ProductDTO product, BigDecimal quantity,
                          BigDecimal discount, LineValues values) {
        BigDecimal unitPrice = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
        model.setValueAt(product, row, 0);
        model.setValueAt(quantity, row, 1);
        model.setValueAt(values.packages(), row, 2);
        model.setValueAt(values.boxes(), row, 3);
        model.setValueAt(values.boxPercentage(), row, 4);
        model.setValueAt(unitPrice, row, 5);
        model.setValueAt(discount == null ? BigDecimal.ZERO : discount, row, 6);
        model.setValueAt(values.total(), row, 7);
    }

    static BigDecimal updateTotalLabel(DefaultTableModel model, JLabel totalLabel) {
        BigDecimal total = BigDecimal.ZERO;
        for (int row = 0; row < model.getRowCount(); row++) {
            total = total.add(decimal(model.getValueAt(row, 7)));
        }
        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", total));
        return total;
    }

    static ProductDTO resolveProduct(java.util.List<ProductDTO> products, mz.multicore.erp.gui.components.ProductSearchComboBox productCombo,
                                     Object cellProduct, Long previousId) {
        ProductDTO product = null;
        if (cellProduct instanceof ProductDTO selected) product = selected;
        else if (productCombo != null) product = productCombo.resolve(cellProduct);
        if (product == null && cellProduct != null) {
            String label = String.valueOf(cellProduct).trim();
            if (!label.isBlank() && !"Pesquisar produto...".equalsIgnoreCase(label)) {
                product = products.stream().filter(p ->
                        label.equalsIgnoreCase(p.name())
                        || (p.reference() != null && label.equalsIgnoreCase(p.reference()))
                        || (p.sku() != null && label.equalsIgnoreCase(p.sku()))
                        || (p.barcode() != null && label.equalsIgnoreCase(p.barcode()))
                ).findFirst().orElse(null);
            }
        }
        if (product == null && previousId != null && previousId > 0) {
            product = products.stream().filter(p -> previousId.equals(p.id())).findFirst().orElse(null);
        }
        return product;
    }

    static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal number) return number;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(value).trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }
}

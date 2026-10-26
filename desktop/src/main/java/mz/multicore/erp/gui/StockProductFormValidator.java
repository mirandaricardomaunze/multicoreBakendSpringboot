package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.quantity.PackagingComposition;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.util.List;

/**
 * Utilitários de sanitização, parsing e validações semânticas de campos de produto.
 */
final class StockProductFormValidator {

    record ProductFormData(
            String sku, String reference, String barcode, String name,
            BigDecimal salesPrice, BigDecimal purchasePrice, BigDecimal minStock,
            PackagingComposition packaging, Long categoryId, Long taxRateId,
            String description, BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netWeightKg, BigDecimal grossWeightKg
    ) {}

    private StockProductFormValidator() {}

    static ProductFormData validateForm(
            JTextField skuField, JTextField refField, JTextField barcodeField, JTextField nameField,
            JTextField salesPriceField, JTextField purchasePriceField, JTextField minStockField,
            JTextField packagesPerBoxField, JTextField unitsPerPackageField, JTextField descField,
            JTextField wholesalePriceField, JTextField wholesaleMinQtyField,
            JTextField netWeightField, JTextField grossWeightField,
            JComboBox<String> categoryCombo, JComboBox<String> taxCombo,
            List<ProductCategoryDTO> categories, List<TaxRateDTO> vatRates
    ) {
        String sku = skuField != null ? skuField.getText().trim() : "";
        String ref = refField.getText().trim();
        String barcode = barcodeField.getText().trim();
        String name = nameField.getText().trim();
        String sPrice = sanitizeNumber(salesPriceField.getText());
        String pPrice = sanitizeNumber(purchasePriceField.getText());
        String mStock = sanitizeNumber(minStockField.getText());
        int pkgs = parseRequiredPositiveInt(packagesPerBoxField.getText(), "embalagens por caixa");
        int units = parseRequiredPositiveInt(unitsPerPackageField.getText(), "unidades por embalagem");
        String desc = descField.getText().trim();

        if ((skuField != null && sku.isEmpty()) || name.isEmpty() || sPrice.isEmpty()) {
            throw new BusinessRuleException("Preencha SKU, Nome e Preço de Venda.");
        }

        BigDecimal salesPrice, purchasePrice, minStock;
        try {
            salesPrice = new BigDecimal(sPrice);
            purchasePrice = pPrice.isEmpty() ? BigDecimal.ZERO : new BigDecimal(pPrice);
            minStock = mStock.isEmpty() ? BigDecimal.ZERO : new BigDecimal(mStock);
        } catch (NumberFormatException nfe) {
            throw new BusinessRuleException("Preços e stock mínimo devem ser números válidos.");
        }

        validatePricesAndStock(salesPrice, purchasePrice, minStock);
        PackagingComposition packaging = PackagingComposition.of(pkgs, units);

        int catIdx = categoryCombo.getSelectedIndex(), taxIdx = taxCombo.getSelectedIndex();
        Long categoryId = (catIdx > 0 && (catIdx - 1) < categories.size()) ? categories.get(catIdx - 1).id() : null;
        Long taxRateId = (taxIdx > 0 && (taxIdx - 1) < vatRates.size()) ? vatRates.get(taxIdx - 1).id() : null;

        BigDecimal wPrice = parsePositiveOrNull(wholesalePriceField.getText());
        BigDecimal wQty = parsePositiveOrNull(wholesaleMinQtyField.getText());
        BigDecimal netWeightKg = parsePositiveOrNull(netWeightField.getText());
        BigDecimal grossWeightKg = parsePositiveOrNull(grossWeightField.getText());
        validateWeights(netWeightKg, grossWeightKg);

        return new ProductFormData(
                sku, ref.isEmpty() ? null : ref, barcode.isEmpty() ? null : barcode, name,
                salesPrice, purchasePrice, minStock, packaging, categoryId, taxRateId,
                desc.isEmpty() ? null : desc, wPrice, wQty, netWeightKg, grossWeightKg
        );
    }

    static int parseIntOrZero(String raw) {
        if (raw == null) return 0;
        try { return Math.max(0, Integer.parseInt(raw.trim())); } catch (NumberFormatException ex) { return 0; }
    }

    static String sanitizeNumber(String raw) {
        if (raw == null) return "";
        String clean = raw.replaceAll("[^0-9,.-]", "").trim();
        if (clean.contains(",") && clean.contains(".")) {
            if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) clean = clean.replace(".", "").replace(',', '.');
            else clean = clean.replace(",", "");
        } else clean = clean.replace(',', '.');
        return clean;
    }

    static BigDecimal parsePositiveOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            BigDecimal v = new BigDecimal(sanitizeNumber(raw));
            return v.signum() > 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static int parseRequiredPositiveInt(String raw, String fieldLabel) {
        try {
            int value = Integer.parseInt(raw == null ? "" : raw.trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("Indique " + fieldLabel + " como número inteiro maior que zero.");
        }
    }

    static void validatePricesAndStock(BigDecimal salesPrice, BigDecimal purchasePrice, BigDecimal minStock) {
        if (salesPrice.signum() < 0) throw new BusinessRuleException("O preço de venda não pode ser negativo.");
        if (purchasePrice.signum() < 0) throw new BusinessRuleException("O preço de compra não pode ser negativo.");
        if (minStock.signum() < 0) throw new BusinessRuleException("O stock mínimo não pode ser negativo.");
    }

    static void validateWeights(BigDecimal netWeightKg, BigDecimal grossWeightKg) {
        if (netWeightKg != null && grossWeightKg != null && grossWeightKg.compareTo(netWeightKg) < 0) {
            throw new BusinessRuleException("O peso bruto deve ser igual ou superior ao peso líquido.");
        }
    }
}

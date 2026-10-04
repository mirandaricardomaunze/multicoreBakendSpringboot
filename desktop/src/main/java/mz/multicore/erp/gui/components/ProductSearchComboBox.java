package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import java.util.Collection;

/** Pesquisa e selecção tipada de produtos nos formulários operacionais. */
public final class ProductSearchComboBox extends SearchableComboBox<ProductDTO> {
    public ProductSearchComboBox() {
        super("Pesquisar nome, referência ou código de barras",
                ProductSearchComboBox::displayLabel,
                ProductSearchComboBox::searchText);
        setToolTipText("Escreva parte do nome, SKU, referência ou código de barras para filtrar os produtos.");
    }

    public void setProducts(Collection<ProductDTO> products) {
        setItems(products);
    }

    public ProductDTO selectedProduct() {
        return selectedValue();
    }

    public boolean selectProduct(Long productId) {
        return productId != null && selectFirst(product -> productId.equals(product.id()));
    }

    public ProductDTO resolve(Object value) {
        if (value instanceof ProductDTO p) return p;
        if (value instanceof Long id) {
            return allItems().stream().filter(p -> id.equals(p.id())).findFirst().orElse(null);
        }
        if (value instanceof String text && !text.isBlank()) {
            String trimmed = text.trim();
            for (ProductDTO item : allItems()) {
                if (trimmed.equalsIgnoreCase(displayLabel(item).trim())
                        || trimmed.equalsIgnoreCase(item.name().trim())
                        || (item.reference() != null && trimmed.equalsIgnoreCase(item.reference().trim()))
                        || (item.sku() != null && trimmed.equalsIgnoreCase(item.sku().trim()))
                        || (item.barcode() != null && trimmed.equalsIgnoreCase(item.barcode().trim()))) {
                    return item;
                }
            }
        }
        return selectedProduct();
    }

    public static javax.swing.table.TableCellEditor createTableCellEditor(ProductSearchComboBox combo) {
        return new javax.swing.DefaultCellEditor(combo) {
            @Override
            public Object getCellEditorValue() {
                ProductDTO p = combo.selectedProduct();
                if (p != null) return p;
                Object val = super.getCellEditorValue();
                return combo.resolve(val);
            }
        };
    }

    public static String displayLabel(ProductDTO product) {
        if (product == null) return "";
        String code = hasText(product.reference()) ? product.reference() : product.sku();
        String barcode = hasText(product.barcode()) ? " | " + product.barcode() : "";
        String price = product.unitPrice() == null ? "" : " - " + product.unitPrice() + " MT";
        return safe(code) + barcode + " - " + safe(product.name()) + price;
    }

    private static String searchText(ProductDTO product) {
        if (product == null) return "";
        return String.join(" ", safe(product.name()), safe(product.sku()), safe(product.reference()),
                safe(product.barcode()), safe(product.description()), safe(product.categoryName()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

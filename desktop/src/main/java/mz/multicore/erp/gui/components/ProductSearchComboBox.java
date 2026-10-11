package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** Pesquisa e selecção tipada de produtos nos formulários operacionais. */
public final class ProductSearchComboBox extends SearchableComboBox<ProductDTO> {
    private Consumer<ProductDTO> onLookupSelected;
    private Runnable onLookupRequested;

    public ProductSearchComboBox() {
        super("Pesquisar nome, referência ou código de barras",
                ProductSearchComboBox::displayLabel,
                ProductSearchComboBox::searchText);
        setToolTipText("Escreva parte do nome, SKU, referência ou código de barras para filtrar os produtos (F3 para catálogo).");
        installLookupTrigger();
    }

    public ProductSearchComboBox(Collection<ProductDTO> products) {
        this();
        if (products != null) {
            setProducts(products);
        }
    }

    private void installLookupTrigger() {
        Component editorComp = getEditor() != null ? getEditor().getEditorComponent() : null;
        if (editorComp instanceof JTextField editorField) {
            JButton lookupBtn = new JButton(UIHelper.icon("fas-search", 12, UIHelper.ACCENT_BLUE));
            lookupBtn.setFocusable(false);
            lookupBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lookupBtn.setToolTipText("Pesquisar no catálogo com paginação (F3)");
            lookupBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            lookupBtn.addActionListener(e -> openLookupDialog());
            editorField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, lookupBtn);

            editorField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "openProductLookup");
            editorField.getActionMap().put("openProductLookup", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openLookupDialog();
                }
            });
        }
    }

    public void setOnLookupSelected(Consumer<ProductDTO> onLookupSelected) {
        this.onLookupSelected = onLookupSelected;
    }

    public void setOnLookupRequested(Runnable onLookupRequested) {
        this.onLookupRequested = onLookupRequested;
    }

    public void openLookupDialog() {
        if (onLookupRequested != null) {
            onLookupRequested.run();
            return;
        }
        Window parent = SwingUtilities.getWindowAncestor(this);
        List<ProductDTO> items = allItems();
        if (items.isEmpty()) return;
        ProductDTO chosen = ProductLookupDialog.show(parent, items);
        if (chosen != null) {
            setSelectedItem(chosen);
            if (onLookupSelected != null) {
                onLookupSelected.accept(chosen);
            }
        }
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
        javax.swing.DefaultCellEditor editor = new javax.swing.DefaultCellEditor(combo) {
            @Override
            public Object getCellEditorValue() {
                ProductDTO p = combo.selectedProduct();
                if (p != null) return p;
                Object val = super.getCellEditorValue();
                return combo.resolve(val);
            }
        };
        combo.setOnLookupSelected(p -> editor.stopCellEditing());
        return editor;
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

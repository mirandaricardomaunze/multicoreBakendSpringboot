package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PricingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.pricing.dto.PriceListDTO;
import mz.multicore.erp.modules.pricing.dto.PriceListItemDTO;
import mz.multicore.erp.modules.pricing.dto.SavePriceListItemRequest;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

/**
 * Product price in a list. It shows the normal price and the difference so the manager sees the discount
 * before saving. Repeating the product with another quantity creates a tier.
 */
final class PriceListItemDialog {

    private PriceListItemDialog() {
    }

    static void show(JComponent parent, PricingApiClient pricingApi, ComercialApiClient comercialApi, PriceListDTO list,
                     PriceListItemDTO existing, Consumer<PriceListItemDTO> onSaved, InlineFeedbackPanel feedback) {
        UIHelper.loadAsync(parent, comercialApi::getAllProducts,
                products -> open(pricingApi, list, products, existing, onSaved),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os produtos", error.getMessage(),
                        "Tentar novamente", () -> show(parent, pricingApi, comercialApi, list, existing, onSaved, feedback)));
    }

    private static void open(PricingApiClient api, PriceListDTO list, List<ProductDTO> products,
                             PriceListItemDTO existing, Consumer<PriceListItemDTO> onSaved) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;

        ProductSearchComboBox productCombo = new ProductSearchComboBox();
        productCombo.setProducts(products);
        if (existing != null) {
            productCombo.selectProduct(existing.productId());
            productCombo.setEnabled(false);
        }
        DecimalField minField = new DecimalField(existing == null ? "0" : ProductUnitsPanel.plain(existing.minQuantity()), 3, false);
        minField.setEnabled(existing == null);
        MoneyField priceField = new MoneyField(existing == null ? "" : ProductUnitsPanel.plain(existing.price()));
        JLabel retailInfo = new JLabel(" ");
        retailInfo.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        retailInfo.setIcon(UIHelper.icon("fas-info-circle", 13, UIHelper.ACCENT_BLUE));
        retailInfo.setIconTextGap(8);

        Runnable sync = () -> {
            ProductDTO product = productCombo.selectedProduct();
            if (product == null || product.unitPrice() == null) {
                retailInfo.setForeground(UIHelper.TEXT_MUTED);
                retailInfo.setText("Escolha o produto para ver o preço normal.");
                return;
            }
            BigDecimal price = ProductUnitFormDialog.parse(priceField.getText());
            String diff = price == null ? "" : "  ·  diferença " + PriceListsPanel.difference(price, product.unitPrice());
            retailInfo.setForeground(UIHelper.TEXT_LIGHT);
            retailInfo.setText("Preço normal: " + UIHelper.formatMzn(product.unitPrice()) + diff);
        };
        productCombo.addActionListener(e -> sync.run());
        UIHelper.onTextChange(priceField, sync);
        sync.run();

        JLabel hint = new JLabel("<html>O preço é por unidade base do produto e na mesma base de IVA do preço normal. "
                + "<b>A partir de</b> cria escalões: por exemplo 0 a 900 MT e 10 a 850 MT.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);

        FormField productForm = new FormField("Produto", productCombo, true, null);
        FormField priceForm = new FormField("Preço da tabela (MT)", priceField, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{
                "", productForm,
                "A partir de (quantidade):", minField,
                "", priceForm,
                "", retailInfo,
                "", hint});

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                existing == null ? "Adicionar Preço" : "Editar Preço", "fas-money-bill-wave",
                "Tabela " + list.name(), form);
        PriceListItemDTO[] result = new PriceListItemDTO[1];
        dialog.setOnSaveAsync(() -> {
            ProductDTO product = productCombo.selectedProduct();
            if (product == null) {
                productForm.setError("Escolha o produto.");
                throw new IllegalArgumentException("Escolha o produto.");
            }
            BigDecimal price = priceField.value();
            if (price.signum() <= 0) {
                UIHelper.markFieldInvalid(priceField, "O preço tem de ser maior que zero.");
                throw new IllegalArgumentException("O preço tem de ser maior que zero.");
            }
            BigDecimal min = minField.getText().isBlank() ? BigDecimal.ZERO : minField.value();
            if (min.signum() < 0) throw new IllegalArgumentException("A quantidade mínima não pode ser negativa.");
            SavePriceListItemRequest request = new SavePriceListItemRequest(companyId, list.id(), product.id(), min, price);
            return () -> { result[0] = api.saveItem(request); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onSaved.accept(result[0]);
    }
}

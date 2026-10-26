package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Diálogo modal para criação de campanhas promocionais de loja. */
final class PromotionFormDialog {

    private PromotionFormDialog() {}

    static void show(JComponent parent, PromotionApiClient promotionApiClient,
                     ComercialApiClient comercialApiClient, Runnable onSuccess,
                     InlineFeedbackPanel feedback) {
        UIHelper.loadAsync(parent,
                () -> new PromotionOptions(comercialApiClient.getAllProducts(),
                        comercialApiClient.getActiveCategories()),
                options -> openDialog(parent, promotionApiClient, options, onSuccess),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar produtos e categorias",
                        error.getMessage(), "Tentar novamente", () -> show(parent, promotionApiClient, comercialApiClient, onSuccess, feedback)));
    }

    private static void openDialog(JComponent parent, PromotionApiClient promotionApiClient,
                                   PromotionOptions options, Runnable onSuccess) {
        List<ProductDTO> products = options.products();
        List<ProductCategoryDTO> categories = options.categories();

        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Percentagem", "Leve X, pague Y"});
        JComboBox<String> scopeCombo = new JComboBox<>(new String[]{"Produto", "Categoria"});
        ProductSearchComboBox productCombo = new ProductSearchComboBox();
        JComboBox<String> categoryCombo = new JComboBox<>();
        productCombo.setProducts(products);
        for (ProductCategoryDTO c : categories) categoryCombo.addItem(c.name());

        MoneyField percentField = new MoneyField();
        QuantityField buyField = new QuantityField("", true);
        QuantityField payField = new QuantityField("", true);
        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now().plusMonths(1));

        UIHelper.styleTextField(nameField);
        UIHelper.styleComboBox(typeCombo);
        UIHelper.styleComboBox(scopeCombo);
        UIHelper.styleComboBox(categoryCombo);

        Runnable syncEnabled = () -> {
            boolean percent = typeCombo.getSelectedIndex() == 0;
            if (!percent) {
                scopeCombo.setSelectedIndex(0);
            }
            scopeCombo.setEnabled(percent);
            boolean byProduct = scopeCombo.getSelectedIndex() == 0;
            productCombo.setEnabled(byProduct);
            categoryCombo.setEnabled(!byProduct);
            percentField.setEnabled(percent);
            buyField.setEnabled(!percent);
            payField.setEnabled(!percent);
        };
        typeCombo.addActionListener(e -> syncEnabled.run());
        scopeCombo.addActionListener(e -> syncEnabled.run());
        syncEnabled.run();

        FormField nameForm = new FormField("Nome", nameField, true, null);
        JPanel form = UIHelper.createDialogForm(
                "", nameForm,
                "Tipo:", typeCombo,
                "Alcance:", scopeCombo,
                "Produto:", productCombo,
                "Categoria:", categoryCombo,
                "Desconto (%):", percentField,
                "Leve (X):", buyField,
                "Pague (Y):", payField,
                "Início (yyyy-MM-dd):", startField,
                "Fim (yyyy-MM-dd):", endField
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Promoção",
                "fas-percent", "Campanha de desconto na loja", form);
        dialog.setOnSaveAsync(() -> {
            if (!nameForm.validateRequired()) throw new IllegalArgumentException("Indique o nome da promoção.");
            boolean percent = typeCombo.getSelectedIndex() == 0;
            boolean byProduct = scopeCombo.getSelectedIndex() == 0;
            ProductDTO selectedProduct = productCombo.selectedProduct();
            int categoryIndex = categoryCombo.getSelectedIndex();
            if (byProduct && selectedProduct == null)
                throw new IllegalArgumentException("Selecione um produto.");
            if (!byProduct && (categoryIndex < 0 || categoryIndex >= categories.size()))
                throw new IllegalArgumentException("Selecione uma categoria.");
            BigDecimal percentValue = percent ? percentField.value() : null;
            Integer buy = percent ? null : buyField.value().intValueExact();
            Integer pay = percent ? null : payField.value().intValueExact();
            LocalDate start = startField.value();
            LocalDate end = endField.value();
            CreatePromotionRequest request = new CreatePromotionRequest(
                    CurrentUserContext.getCurrentCompanyId(), nameField.getText().trim(),
                    percent ? "PERCENT" : "BUY_X_GET_Y",
                    byProduct ? selectedProduct.id() : null,
                    byProduct ? null : categories.get(categoryIndex).id(),
                    percentValue, buy, pay, start, end);
            return () -> { promotionApiClient.createPromotion(request); return null; };
        });
        if (dialog.showDialog()) {
            onSuccess.run();
            ToastManager.success(parent, "Promoção criada.");
        }
    }

    private record PromotionOptions(List<ProductDTO> products, List<ProductCategoryDTO> categories) {}
}

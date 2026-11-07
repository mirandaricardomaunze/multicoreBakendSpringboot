package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.gui.StockProductFormValidator.ProductFormData;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;

import javax.swing.*;
import java.util.List;

/**
 * Diálogos modais corporativos para criação e edição de artigos no catálogo de produtos.
 */
final class StockProductFormDialog {

    private StockProductFormDialog() {}

    static void showCreate(StockPanel owner, List<ProductCategoryDTO> categories, List<TaxRateDTO> vatRates) {
        JTextField skuField = new JTextField(), referenceField = new JTextField(), barcodeField = new JTextField();
        JTextField nameField = new JTextField(), salesPriceField = new JTextField(), purchasePriceField = new JTextField();
        JTextField minStockField = new JTextField("0"), packagesPerBoxField = new JTextField("1"), unitsPerPackageField = new JTextField("1");
        JTextField unitsPerBoxField = new JTextField("1"); unitsPerBoxField.setEditable(false);
        JTextField netWeightField = new JTextField(), grossWeightField = new JTextField();
        JTextField wholesalePriceField = new JTextField(), wholesaleMinQtyField = new JTextField(), descField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>();

        for (JTextField f : List.of(skuField, referenceField, barcodeField, nameField, salesPriceField,
                purchasePriceField, minStockField, packagesPerBoxField, unitsPerPackageField, unitsPerBoxField,
                netWeightField, grossWeightField, wholesalePriceField, wholesaleMinQtyField, descField)) UIHelper.styleTextField(f);
        UIHelper.styleComboBox(categoryCombo);
        wholesalePriceField.putClientProperty("JTextField.placeholderText", "Opcional - preço ao grosso");
        wholesaleMinQtyField.putClientProperty("JTextField.placeholderText", "Qtd (unidades) a partir da qual aplica");
        StockProductFormBuilder.bindPackagingFields(packagesPerBoxField, unitsPerPackageField, unitsPerBoxField);

        categoryCombo.addItem("Sem categoria");
        for (var c : categories) categoryCombo.addItem(c.name() + "  (" + c.code() + ")");

        JComboBox<String> taxCombo = new JComboBox<>();
        UIHelper.styleComboBox(taxCombo);
        taxCombo.addItem("IVA Padrão (16%)");
        int defaultTaxIdx = 0;
        for (int i = 0; i < vatRates.size(); i++) {
            taxCombo.addItem(vatRates.get(i).name());
            if ("IVA_STANDARD".equals(vatRates.get(i).type())) defaultTaxIdx = i + 1;
        }
        if (taxCombo.getItemCount() > 0) taxCombo.setSelectedIndex(defaultTaxIdx);

        final byte[][] imageHolder = {null};
        JPanel imagePanel = StockProductFormBuilder.createImageChooserPanel(owner, imageHolder); JCheckBox perishableBox = StockProductFormBuilder.perishableCheckBox(); // validade obrigatória se ligado

        JPanel dialogPanel = UIHelper.createDialogForm(
                "SKU / Código (Único):", skuField, "Referência:", referenceField, "Código de Barras:", barcodeField,
                "Nome do Produto:", nameField, "Categoria:", categoryCombo, "Taxa de IVA:", taxCombo,
                "Preço de Venda (MT):", salesPriceField, "Preço de Compra (MT):", purchasePriceField, "Stock Mínimo:", minStockField,
                "Embalagens por Caixa:", packagesPerBoxField, "Unidades por Embalagem:", unitsPerPackageField,
                "Total de Unidades por Caixa:", unitsPerBoxField, "Peso líquido/unidade (kg):", netWeightField,
                "Peso bruto/unidade (kg):", grossWeightField, "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField, "Validade:", perishableBox, "Descrição:", descField, "Imagem (opcional):", imagePanel
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Registar Novo Produto", "fas-boxes",
                "Defina os dados e o IVA do artigo", dialogPanel).setConfirmButton("Registar Produto", "fas-save");

        final ProductDTO[] createdHolder = {null};
        dialog.setOnSaveAsync(() -> {
            ProductFormData data = StockProductFormValidator.validateForm(
                    skuField, referenceField, barcodeField, nameField, salesPriceField, purchasePriceField,
                    minStockField, packagesPerBoxField, unitsPerPackageField, descField, wholesalePriceField,
                    wholesaleMinQtyField, netWeightField, grossWeightField, categoryCombo, taxCombo, categories, vatRates);
            byte[] selectedImage = imageHolder[0]; boolean perishable = perishableBox.isSelected();

            return () -> {
                ProductDTO created = owner.comercialApiClient.createProduct(
                        data.sku(), data.reference(), data.barcode(), data.name(), data.salesPrice(), data.purchasePrice(),
                        data.minStock(), data.packaging().unitsPerBox(), data.packaging().packagesPerBox(), data.packaging().unitsPerPackage(),
                        data.categoryId(), "UNIT", true, data.taxRateId(), data.description(),
                        data.wholesalePrice(), data.wholesaleMinQty(), data.netWeightKg(), data.grossWeightKg(), null, perishable);
                if (selectedImage != null) owner.comercialApiClient.updateProductImage(created.id(), selectedImage);
                createdHolder[0] = created;
                return created;
            };
        });

        if (dialog.showDialog()) {
            owner.onPanelSelected();
            ProductDTO created = createdHolder[0];
            if (created != null && ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                    "Adicionar Stock Inicial", "Produto '" + created.name() + "' cadastrado com sucesso.\nDeseja adicionar stock inicial agora?", "Adicionar Stock")) {
                StockBatchEntryDialog.show(owner, created);
            }
        }
    }

    static void showEdit(StockPanel owner, Long preselectedProductId, List<ProductCategoryDTO> categories,
                         List<TaxRateDTO> vatRates, List<ProductDTO> products) {
        if (products.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Sem produtos", "Não existem produtos registados para editar.");
            return;
        }

        ProductSearchComboBox productCombo = new ProductSearchComboBox();
        JTextField skuField = new JTextField(); skuField.setEditable(false);
        JTextField referenceField = new JTextField(), barcodeField = new JTextField(), nameField = new JTextField();
        JTextField salesPriceField = new JTextField(), purchasePriceField = new JTextField(), minStockField = new JTextField();
        JTextField packagesPerBoxField = new JTextField(), unitsPerPackageField = new JTextField(), unitsPerBoxField = new JTextField();
        unitsPerBoxField.setEditable(false);
        JTextField netWeightField = new JTextField(), grossWeightField = new JTextField(), wholesalePriceField = new JTextField();
        JTextField wholesaleMinQtyField = new JTextField(), descField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>(), taxCombo = new JComboBox<>();

        for (JTextField f : List.of(skuField, referenceField, barcodeField, nameField, salesPriceField,
                purchasePriceField, minStockField, packagesPerBoxField, unitsPerPackageField, unitsPerBoxField,
                netWeightField, grossWeightField, wholesalePriceField, wholesaleMinQtyField, descField)) UIHelper.styleTextField(f);
        UIHelper.styleComboBox(categoryCombo); UIHelper.styleComboBox(taxCombo);
        StockProductFormBuilder.bindPackagingFields(packagesPerBoxField, unitsPerPackageField, unitsPerBoxField);
        productCombo.setProducts(products);
        if (preselectedProductId != null) productCombo.selectProduct(preselectedProductId);

        categoryCombo.addItem("Sem categoria");
        for (var c : categories) categoryCombo.addItem(c.name() + "  (" + c.code() + ")");
        taxCombo.addItem("IVA Padrão (16%)");
        for (var r : vatRates) taxCombo.addItem(r.name());

        final byte[][] imageHolder = {null};
        JPanel imagePanel = StockProductFormBuilder.createImageChooserPanel(owner, imageHolder); JCheckBox perishableBox = StockProductFormBuilder.perishableCheckBox(); // validade obrigatória se ligado

        Runnable prefill = () -> {
            ProductDTO p = productCombo.selectedProduct();
            if (p == null) return;
            skuField.setText(p.sku()); referenceField.setText(p.reference() == null ? "" : p.reference()); barcodeField.setText(p.barcode() == null ? "" : p.barcode());
            nameField.setText(p.name()); salesPriceField.setText(p.unitPrice() == null ? "" : p.unitPrice().toPlainString());
            purchasePriceField.setText(p.purchasePrice() == null ? "0" : p.purchasePrice().toPlainString()); minStockField.setText(p.minStock() == null ? "0" : p.minStock().toPlainString());
            packagesPerBoxField.setText(String.valueOf(p.packagesPerBox())); unitsPerPackageField.setText(String.valueOf(p.unitsPerPackage())); unitsPerBoxField.setText(String.valueOf(p.unitsPerBox()));
            netWeightField.setText(p.netUnitWeightKg() == null ? "" : p.netUnitWeightKg().toPlainString()); grossWeightField.setText(p.grossUnitWeightKg() == null ? "" : p.grossUnitWeightKg().toPlainString());
            wholesalePriceField.setText(p.wholesalePrice() == null ? "" : p.wholesalePrice().toPlainString()); wholesaleMinQtyField.setText(p.wholesaleMinQty() == null ? "" : p.wholesaleMinQty().toPlainString());
            descField.setText(p.description() == null ? "" : p.description()); perishableBox.setSelected(p.tracksExpiry());
            categoryCombo.setSelectedIndex(0);
            if (p.categoryId() != null) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).id().equals(p.categoryId())) { categoryCombo.setSelectedIndex(i + 1); break; }
                }
            }
            taxCombo.setSelectedIndex(0);
            if (p.taxRateId() != null) {
                for (int i = 0; i < vatRates.size(); i++) {
                    if (vatRates.get(i).id().equals(p.taxRateId())) { taxCombo.setSelectedIndex(i + 1); break; }
                }
            }
            imageHolder[0] = null;
        };
        productCombo.addActionListener(e -> prefill.run());
        prefill.run();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Produto a Editar:", productCombo, "SKU (Identificador único):", skuField, "Referência:", referenceField,
                "Código de Barras:", barcodeField, "Nome do Produto:", nameField, "Categoria:", categoryCombo,
                "Taxa de IVA:", taxCombo, "Preço de Venda (MT):", salesPriceField, "Preço de Compra (MT):", purchasePriceField,
                "Stock Mínimo:", minStockField, "Embalagens por Caixa:", packagesPerBoxField, "Unidades por Embalagem:", unitsPerPackageField,
                "Total de Unidades por Caixa:", unitsPerBoxField, "Peso líquido/unidade (kg):", netWeightField,
                "Peso bruto/unidade (kg):", grossWeightField, "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField, "Validade:", perishableBox, "Descrição:", descField, "Nova Imagem (opcional):", imagePanel
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Editar Produto", "fas-edit",
                "Actualize os dados do artigo", dialogPanel).setConfirmButton("Actualizar", "fas-save");

        dialog.setOnSaveAsync(() -> {
            ProductDTO selected = productCombo.selectedProduct();
            if (selected == null) throw new BusinessRuleException("Selecione um produto válido.");
            ProductFormData data = StockProductFormValidator.validateForm(
                    null, referenceField, barcodeField, nameField, salesPriceField, purchasePriceField,
                    minStockField, packagesPerBoxField, unitsPerPackageField, descField, wholesalePriceField,
                    wholesaleMinQtyField, netWeightField, grossWeightField, categoryCombo, taxCombo, categories, vatRates);
            byte[] selectedImage = imageHolder[0]; boolean perishable = perishableBox.isSelected();

            return () -> {
                owner.comercialApiClient.updateProduct(
                        selected.id(), data.reference(), data.barcode(), data.name(), data.salesPrice(), data.purchasePrice(),
                        data.minStock(), data.packaging().unitsPerBox(), data.packaging().packagesPerBox(), data.packaging().unitsPerPackage(),
                        data.categoryId(), selected.saleType() != null ? selected.saleType() : "UNIT", selected.stockTracked(), data.taxRateId(),
                        data.description(), data.wholesalePrice(), data.wholesaleMinQty(), data.netWeightKg(), data.grossWeightKg(), selected.businessUnitId(), perishable);
                if (selectedImage != null) owner.comercialApiClient.updateProductImage(selected.id(), selectedImage);
                return null;
            };
        });

        if (dialog.showDialog()) {
            owner.onPanelSelected();
            owner.showStockSuccess("Produto actualizado com sucesso.");
        }
    }
}

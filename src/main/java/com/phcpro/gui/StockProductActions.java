package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.*;
import com.phcpro.modules.inventory.dto.*;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Diálogos de catálogo e lotes do módulo de stock. */
final class StockProductActions {
    private final StockPanel owner;
    StockProductActions(StockPanel owner) { this.owner = owner; }

    private static int parseIntOrZero(String raw) {
        if (raw == null) return 0;
        try {
            int v = Integer.parseInt(raw.trim());
            return Math.max(0, v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** Decimal > 0 a partir de texto livre; vazio/inválido/≤0 → null (campos opcionais de grosso). */
    private static BigDecimal parsePositiveOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            BigDecimal v = new BigDecimal(raw.trim());
            return v.signum() > 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Decimal ≥ 0 a partir de texto livre; vazio/inválido → 0 (para unidades soltas). */
    private static BigDecimal parseDecimalOrZero(String raw) {
        if (raw == null || raw.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal v = new BigDecimal(raw.trim());
            return v.signum() < 0 ? BigDecimal.ZERO : v;
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    public void createBatchEntryDialog(ProductDTO preselected) {
        List<ProductDTO> products = new ArrayList<>(owner.catalogProducts);
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre primeiro um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (owner.warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Crie primeiro um armazém.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> prodCombo = new JComboBox<>();
        JComboBox<String> whCombo = new JComboBox<>();
        // Entrada por caixas: nº de caixas + unidades soltas → total em unidades (read-only).
        // O stock é sempre persistido/movimentado em UNIDADES; a caixa é só camada de entrada.
        JTextField boxesField = new JTextField("0");
        JTextField looseField = new JTextField("0");
        JTextField totalUnitsField = new JTextField();
        totalUnitsField.setEditable(false);
        JLabel unitsPerBoxHint = new JLabel(" ");
        unitsPerBoxHint.setForeground(UIHelper.TEXT_MUTED);
        JTextField expirationField = new JTextField();
        JTextField batchField = new JTextField();
        JTextField serialField = new JTextField();
        JTextField descField = new JTextField("Entrada de lote/validade");

        UIHelper.styleComboBox(prodCombo);
        UIHelper.styleComboBox(whCombo);
        UIHelper.styleTextField(boxesField);
        UIHelper.styleTextField(looseField);
        UIHelper.styleTextField(totalUnitsField);
        UIHelper.styleTextField(expirationField);
        UIHelper.styleTextField(batchField);
        UIHelper.styleTextField(serialField);
        UIHelper.styleTextField(descField);

        expirationField.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd (ex: 2027-12-31)");
        batchField.putClientProperty("JTextField.placeholderText", "Opcional — gerado a partir da validade se vazio");
        serialField.putClientProperty("JTextField.placeholderText", "Opcional");

        for (ProductDTO p : products) {
            prodCombo.addItem(p.sku() + " — " + p.name());
        }
        for (WarehouseDTO w : owner.warehousesList) {
            whCombo.addItem(w.name());
        }
        if (preselected != null) {
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).id().equals(preselected.id())) {
                    prodCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Total (unidades) = nº caixas × unidades/caixa + unidades soltas. Recalcula ao mudar
        // produto (logo unidades/caixa), nº de caixas ou unidades soltas.
        Runnable recomputeTotal = () -> {
            int idx = prodCombo.getSelectedIndex();
            int upb = (idx >= 0 && idx < products.size())
                    ? Math.max(1, products.get(idx).unitsPerBox()) : 1;
            unitsPerBoxHint.setText(upb + " unidade(s) por caixa");
            int boxes = parseIntOrZero(boxesField.getText());
            BigDecimal loose = parseDecimalOrZero(looseField.getText());
            BigDecimal total = BigDecimal.valueOf((long) boxes * upb).add(loose);
            totalUnitsField.setText(total.stripTrailingZeros().toPlainString());
        };
        prodCombo.addActionListener(e -> recomputeTotal.run());
        UIHelper.onTextChange(boxesField, recomputeTotal);
        UIHelper.onTextChange(looseField, recomputeTotal);
        recomputeTotal.run();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Produto:", prodCombo,
                "Armazém:", whCombo,
                "Nº de Caixas:", boxesField,
                "Unidades soltas:", looseField,
                "Unidades / caixa:", unitsPerBoxHint,
                "Total (unidades):", totalUnitsField,
                "Validade (yyyy-MM-dd):", expirationField,
                "Nº Lote:", batchField,
                "Nº Série:", serialField,
                "Descrição:", descField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Adicionar Lote / Validade", "fas-boxes", "Registe lote e data de validade (FEFO)", dialogPanel).showDialog();
        if (!confirmed) return;

        int prodIdx = prodCombo.getSelectedIndex();
        int whIdx = whCombo.getSelectedIndex();
        if (prodIdx < 0 || whIdx < 0) return;

        BigDecimal qty;
        try {
            // A quantidade gravada é o total em unidades (caixas × und/caixa + soltas).
            qty = new BigDecimal(totalUnitsField.getText().trim());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(owner, "Quantidade deve ser maior que zero. Indique o nº de caixas e/ou unidades soltas.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String expRaw = expirationField.getText().trim();
        if (expRaw.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Validade é obrigatória (formato yyyy-MM-dd).", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate expirationDate;
        try {
            expirationDate = LocalDate.parse(expRaw);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(owner, "Validade inválida. Use o formato yyyy-MM-dd (ex: 2027-12-31).", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (expirationDate.isBefore(LocalDate.now())) {
            int confirm = JOptionPane.showConfirmDialog(owner,
                    "A validade já está expirada. Pretende registar mesmo assim?",
                    "Confirmação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        String batch = batchField.getText().trim();
        if (batch.isEmpty()) batch = null;
        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;
        String desc = descField.getText().trim();

        ProductDTO selectedDTO = products.get(prodIdx);
        WarehouseDTO selectedWarehouse = owner.warehousesList.get(whIdx);

        RegisterMovementRequest request = new RegisterMovementRequest(
                    selectedDTO.id(), selectedWarehouse.id(), qty, "ENTRY",
                    batch, serial, desc, expirationDate);
        UIHelper.runWithProgress(owner, "A registar entrada de lote…", () -> owner.inventoryApiClient.registerMovement(request), ignored -> {
            JOptionPane.showMessageDialog(owner,
                    "Lote registado com sucesso para '" + selectedDTO.name() + "'.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            owner.onPanelSelected();
        }, owner::showStockError);
    }

    public void createProductDialog() {
        loadProductOptions(options -> showCreateProductDialog(options.categories(), options.vatRates()));
    }

    private void showCreateProductDialog(
            java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categories,
            java.util.List<com.phcpro.modules.fiscal.dto.TaxRateDTO> vatRates) {
        JTextField skuField = new JTextField();
        JTextField referenceField = new JTextField();
        JTextField barcodeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField salesPriceField = new JTextField();
        JTextField purchasePriceField = new JTextField();
        JTextField minStockField = new JTextField("0");
        JTextField unitsPerBoxField = new JTextField("1");
        JTextField wholesalePriceField = new JTextField();
        JTextField wholesaleMinQtyField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>();

        UIHelper.styleTextField(skuField);
        UIHelper.styleTextField(referenceField);
        UIHelper.styleTextField(barcodeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(salesPriceField);
        UIHelper.styleTextField(purchasePriceField);
        UIHelper.styleTextField(minStockField);
        UIHelper.styleTextField(unitsPerBoxField);
        UIHelper.styleTextField(wholesalePriceField);
        UIHelper.styleTextField(wholesaleMinQtyField);
        UIHelper.styleTextField(descField);
        UIHelper.styleComboBox(categoryCombo);
        wholesalePriceField.putClientProperty("JTextField.placeholderText", "Opcional — preço ao grosso");
        wholesaleMinQtyField.putClientProperty("JTextField.placeholderText", "Qtd (unidades) a partir da qual aplica");

        categoryCombo.addItem("— Sem categoria —");
        for (var c : categories) categoryCombo.addItem(c.name() + "  (" + c.code() + ")");

        // IVA dinâmico: taxa de IVA por produto (default = IVA Normal 16%).
        JComboBox<String> taxCombo = new JComboBox<>();
        UIHelper.styleComboBox(taxCombo);
        int defaultTaxIdx = 0;
        for (int i = 0; i < vatRates.size(); i++) {
            taxCombo.addItem(vatRates.get(i).name());
            if ("IVA_STANDARD".equals(vatRates.get(i).type())) defaultTaxIdx = i;
        }
        if (taxCombo.getItemCount() > 0) taxCombo.setSelectedIndex(defaultTaxIdx);

        // Selector de imagem (opcional) — guardada como thumbnail na BD para o catálogo POS em cards.
        final byte[][] imageHolder = {null};
        JLabel imagePreview = new JLabel("Sem imagem", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(96, 96));
        imagePreview.setOpaque(true);
        imagePreview.setBackground(UIHelper.BG_CARD);
        imagePreview.setForeground(UIHelper.TEXT_MUTED);
        imagePreview.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1, true));
        ModernButton chooseImageBtn = UIHelper.createSecondaryButton("Escolher Imagem…");
        chooseImageBtn.setIcon(UIHelper.icon("fas-image", 14));
        chooseImageBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens (png, jpg)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
                byte[] bytes = UIHelper.readScaledImage(fc.getSelectedFile(), 320);
                if (bytes == null) {
                    JOptionPane.showMessageDialog(owner, "Não foi possível ler a imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                imageHolder[0] = bytes;
                imagePreview.setText(null);
                imagePreview.setIcon(UIHelper.imageIconFromBytes(bytes, 96, 96));
            }
        });
        JPanel imagePanel = new JPanel(new BorderLayout(10, 0));
        imagePanel.setOpaque(false);
        imagePanel.add(imagePreview, BorderLayout.WEST);
        imagePanel.add(chooseImageBtn, BorderLayout.CENTER);

        JPanel dialogPanel = UIHelper.createDialogForm(
                "SKU / Codigo (Unico):", skuField,
                "Referencia:", referenceField,
                "Codigo de Barras:", barcodeField,
                "Nome do Produto:", nameField,
                "Categoria:", categoryCombo,
                "Taxa de IVA:", taxCombo,
                "Preço de Venda (MT):", salesPriceField,
                "Preço de Compra (MT):", purchasePriceField,
                "Stock Mínimo:", minStockField,
                "Unidades por Caixa:", unitsPerBoxField,
                "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField,
                "Descrição:", descField,
                "Imagem (opcional):", imagePanel
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Registar Novo Produto", "fas-boxes", "Defina os dados e o IVA do artigo", dialogPanel).showDialog();
        if (confirmed) {
            String sku = skuField.getText().trim();
            String reference = referenceField.getText().trim();
            String barcode = barcodeField.getText().trim();
            String name = nameField.getText().trim();
            String salesPriceStr = salesPriceField.getText().trim();
            String purchasePriceStr = purchasePriceField.getText().trim();
            String minStockStr = minStockField.getText().trim();
            String unitsPerBoxStr = unitsPerBoxField.getText().trim();
            String desc = descField.getText().trim();

            if (sku.isEmpty() || name.isEmpty() || salesPriceStr.isEmpty() || purchasePriceStr.isEmpty()) {
                JOptionPane.showMessageDialog(owner, "SKU, Nome, Preço de Venda e Preço de Compra são campos obrigatórios.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                BigDecimal salesPrice = new BigDecimal(salesPriceStr);
                BigDecimal purchasePrice = new BigDecimal(purchasePriceStr);
                BigDecimal minStock = new BigDecimal(minStockStr);
                int unitsPerBox;
                try {
                    unitsPerBox = unitsPerBoxStr.isEmpty() ? 1 : Integer.parseInt(unitsPerBoxStr);
                    if (unitsPerBox < 1) unitsPerBox = 1;
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(owner, "Unidades por caixa deve ser um número inteiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int catIdx = categoryCombo.getSelectedIndex();
                Long categoryId = null;
                if (catIdx > 0 && (catIdx - 1) < categories.size()) {
                    categoryId = categories.get(catIdx - 1).id();
                }
                Long taxRateId = null;
                int taxIdx = taxCombo.getSelectedIndex();
                if (taxIdx >= 0 && taxIdx < vatRates.size()) {
                    taxRateId = vatRates.get(taxIdx).id();
                }
                BigDecimal wholesalePrice = parsePositiveOrNull(wholesalePriceField.getText());
                BigDecimal wholesaleMinQty = parsePositiveOrNull(wholesaleMinQtyField.getText());

                Long selectedCategoryId = categoryId;
                Long selectedTaxRateId = taxRateId;
                int selectedUnitsPerBox = unitsPerBox;
                byte[] selectedImage = imageHolder[0];
                UIHelper.runWithProgress(owner, "A registar produto…", () -> {
                    ProductDTO created = owner.comercialApiClient.createProduct(
                            sku, reference.isEmpty() ? null : reference,
                            barcode.isEmpty() ? null : barcode, name, salesPrice, purchasePrice,
                            minStock, selectedUnitsPerBox, selectedCategoryId, "UNIT", true,
                            selectedTaxRateId, desc.isEmpty() ? null : desc,
                            wholesalePrice, wholesaleMinQty);
                    if (selectedImage != null) {
                        owner.comercialApiClient.updateProductImage(created.id(), selectedImage);
                    }
                    return created;
                }, created -> {
                    owner.onPanelSelected();
                    int addStock = JOptionPane.showConfirmDialog(owner,
                        "Produto '" + name + "' cadastrado.\nDeseja adicionar stock inicial com validade agora?",
                        "Adicionar stock inicial", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (addStock == JOptionPane.YES_OPTION) createBatchEntryDialog(created);
                }, owner::showStockError);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(owner, "Os valores de preço e stock mínimo devem ser numéricos.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, "Erro ao registar produto: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Editar um produto existente: selecciona-se o artigo num combo e o formulário pré-preenche-se.
     * O SKU é imutável (identidade); os restantes dados — incluindo unidades/caixa e IVA — são
     * actualizáveis. Não mexe no stock. Delega em {@code ComercialApiClient.updateProduct}.
     */
    public void editProductDialog(Long preselectedProductId) {
        loadProductOptions(options -> showEditProductDialog(
                preselectedProductId, options.categories(), options.vatRates()));
    }

    private void showEditProductDialog(Long preselectedProductId,
            java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categories,
            java.util.List<com.phcpro.modules.fiscal.dto.TaxRateDTO> vatRates) {
        java.util.List<ProductDTO> products = new ArrayList<>(owner.catalogProducts);
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre primeiro um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> productCombo = new JComboBox<>();
        JTextField skuField = new JTextField();
        skuField.setEditable(false);
        JTextField referenceField = new JTextField();
        JTextField barcodeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField salesPriceField = new JTextField();
        JTextField purchasePriceField = new JTextField();
        JTextField minStockField = new JTextField("0");
        JTextField unitsPerBoxField = new JTextField("1");
        JTextField wholesalePriceField = new JTextField();
        JTextField wholesaleMinQtyField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>();

        UIHelper.styleComboBox(productCombo);
        UIHelper.styleTextField(wholesalePriceField);
        UIHelper.styleTextField(wholesaleMinQtyField);
        UIHelper.styleTextField(skuField);
        UIHelper.styleTextField(referenceField);
        UIHelper.styleTextField(barcodeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(salesPriceField);
        UIHelper.styleTextField(purchasePriceField);
        UIHelper.styleTextField(minStockField);
        UIHelper.styleTextField(unitsPerBoxField);
        UIHelper.styleTextField(descField);
        UIHelper.styleComboBox(categoryCombo);

        for (ProductDTO p : products) productCombo.addItem(p.sku() + " — " + p.name());
        // Abre já no produto seleccionado no inventário (ou no primeiro, se nenhum).
        if (preselectedProductId != null) {
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).id().equals(preselectedProductId)) { productCombo.setSelectedIndex(i); break; }
            }
        }

        categoryCombo.addItem("— Sem categoria —");
        for (var c : categories) categoryCombo.addItem(c.name() + "  (" + c.code() + ")");

        JComboBox<String> taxCombo = new JComboBox<>();
        UIHelper.styleComboBox(taxCombo);
        for (var r : vatRates) taxCombo.addItem(r.name());

        final byte[][] imageHolder = {null};
        JLabel imagePreview = new JLabel("Sem imagem", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(96, 96));
        imagePreview.setOpaque(true);
        imagePreview.setBackground(UIHelper.BG_CARD);
        imagePreview.setForeground(UIHelper.TEXT_MUTED);
        imagePreview.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1, true));
        ModernButton chooseImageBtn = UIHelper.createSecondaryButton("Escolher Imagem…");
        chooseImageBtn.setIcon(UIHelper.icon("fas-image", 14));
        chooseImageBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens (png, jpg)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
                byte[] bytes = UIHelper.readScaledImage(fc.getSelectedFile(), 320);
                if (bytes == null) {
                    JOptionPane.showMessageDialog(owner, "Não foi possível ler a imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                imageHolder[0] = bytes;
                imagePreview.setText(null);
                imagePreview.setIcon(UIHelper.imageIconFromBytes(bytes, 96, 96));
            }
        });
        JPanel imagePanel = new JPanel(new BorderLayout(10, 0));
        imagePanel.setOpaque(false);
        imagePanel.add(imagePreview, BorderLayout.WEST);
        imagePanel.add(chooseImageBtn, BorderLayout.CENTER);

        // Pré-preenche o formulário com o produto seleccionado (e limpa imagem por enviar).
        Runnable prefill = () -> {
            int idx = productCombo.getSelectedIndex();
            if (idx < 0 || idx >= products.size()) return;
            ProductDTO p = products.get(idx);
            skuField.setText(p.sku());
            referenceField.setText(p.reference() == null ? "" : p.reference());
            barcodeField.setText(p.barcode() == null ? "" : p.barcode());
            nameField.setText(p.name());
            salesPriceField.setText(p.unitPrice() == null ? "" : p.unitPrice().toPlainString());
            purchasePriceField.setText(p.purchasePrice() == null ? "0" : p.purchasePrice().toPlainString());
            minStockField.setText(p.minStock() == null ? "0" : p.minStock().toPlainString());
            unitsPerBoxField.setText(String.valueOf(p.unitsPerBox()));
            wholesalePriceField.setText(p.wholesalePrice() == null ? "" : p.wholesalePrice().toPlainString());
            wholesaleMinQtyField.setText(p.wholesaleMinQty() == null ? "" : p.wholesaleMinQty().toPlainString());
            descField.setText(p.description() == null ? "" : p.description());

            categoryCombo.setSelectedIndex(0);
            if (p.categoryId() != null) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).id().equals(p.categoryId())) { categoryCombo.setSelectedIndex(i + 1); break; }
                }
            }
            if (p.taxRateId() != null) {
                for (int i = 0; i < vatRates.size(); i++) {
                    if (vatRates.get(i).id().equals(p.taxRateId())) { taxCombo.setSelectedIndex(i); break; }
                }
            }
            imageHolder[0] = null; // só reenvia imagem se o operador escolher uma nova
            if (p.image() != null && p.image().length > 0) {
                imagePreview.setText(null);
                imagePreview.setIcon(UIHelper.imageIconFromBytes(p.image(), 96, 96));
            } else {
                imagePreview.setIcon(null);
                imagePreview.setText("Sem imagem");
            }
        };
        productCombo.addActionListener(e -> prefill.run());
        prefill.run();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Produto:", productCombo,
                "SKU / Codigo:", skuField,
                "Referencia:", referenceField,
                "Codigo de Barras:", barcodeField,
                "Nome do Produto:", nameField,
                "Categoria:", categoryCombo,
                "Taxa de IVA:", taxCombo,
                "Preço de Venda (MT):", salesPriceField,
                "Preço de Compra (MT):", purchasePriceField,
                "Stock Mínimo:", minStockField,
                "Unidades por Caixa:", unitsPerBoxField,
                "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField,
                "Descrição:", descField,
                "Imagem (opcional):", imagePanel
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Editar Produto", "fas-edit", "Actualize os dados do artigo", dialogPanel).showDialog();
        if (!confirmed) return;

        int idx = productCombo.getSelectedIndex();
        if (idx < 0 || idx >= products.size()) return;
        ProductDTO selected = products.get(idx);

        String reference = referenceField.getText().trim();
        String barcode = barcodeField.getText().trim();
        String name = nameField.getText().trim();
        String salesPriceStr = salesPriceField.getText().trim();
        String purchasePriceStr = purchasePriceField.getText().trim();
        String minStockStr = minStockField.getText().trim();
        String unitsPerBoxStr = unitsPerBoxField.getText().trim();
        String desc = descField.getText().trim();

        if (name.isEmpty() || salesPriceStr.isEmpty() || purchasePriceStr.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Nome, Preço de Venda e Preço de Compra são campos obrigatórios.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            BigDecimal salesPrice = new BigDecimal(salesPriceStr);
            BigDecimal purchasePrice = new BigDecimal(purchasePriceStr);
            BigDecimal minStock = new BigDecimal(minStockStr.isEmpty() ? "0" : minStockStr);
            int unitsPerBox;
            try {
                unitsPerBox = unitsPerBoxStr.isEmpty() ? 1 : Integer.parseInt(unitsPerBoxStr);
                if (unitsPerBox < 1) unitsPerBox = 1;
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(owner, "Unidades por caixa deve ser um número inteiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int catIdx = categoryCombo.getSelectedIndex();
            Long categoryId = null;
            if (catIdx > 0 && (catIdx - 1) < categories.size()) {
                categoryId = categories.get(catIdx - 1).id();
            }
            Long taxRateId = null;
            int taxIdx = taxCombo.getSelectedIndex();
            if (taxIdx >= 0 && taxIdx < vatRates.size()) {
                taxRateId = vatRates.get(taxIdx).id();
            }

            BigDecimal wholesalePrice = parsePositiveOrNull(wholesalePriceField.getText());
            BigDecimal wholesaleMinQty = parsePositiveOrNull(wholesaleMinQtyField.getText());

            Long selectedCategoryId = categoryId;
            Long selectedTaxRateId = taxRateId;
            int selectedUnitsPerBox = unitsPerBox;
            byte[] selectedImage = imageHolder[0];
            UIHelper.runWithProgress(owner, "A actualizar produto…", () -> {
                owner.comercialApiClient.updateProduct(
                        selected.id(), reference.isEmpty() ? null : reference,
                        barcode.isEmpty() ? null : barcode, name, salesPrice, purchasePrice,
                        minStock, selectedUnitsPerBox, selectedCategoryId, selected.saleType(),
                        selected.stockTracked(), selectedTaxRateId,
                        desc.isEmpty() ? null : desc, wholesalePrice, wholesaleMinQty);
                if (selectedImage != null) {
                    owner.comercialApiClient.updateProductImage(selected.id(), selectedImage);
                }
                return null;
            }, ignored -> {
                owner.onPanelSelected();
                JOptionPane.showMessageDialog(owner, "Produto '" + name + "' actualizado com sucesso.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }, owner::showStockError);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner, "Os valores de preço e stock mínimo devem ser numéricos.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Erro ao actualizar produto: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProductOptions(java.util.function.Consumer<ProductOptions> onLoaded) {
        UIHelper.loadAsync(owner, () -> new ProductOptions(
                        owner.comercialApiClient.getActiveCategories(),
                        owner.comercialApiClient.getActiveVatRates()),
                onLoaded, owner::showStockError);
    }

    private record ProductOptions(
            java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categories,
            java.util.List<com.phcpro.modules.fiscal.dto.TaxRateDTO> vatRates) {}
}

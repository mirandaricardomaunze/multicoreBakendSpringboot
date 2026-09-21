package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.inventory.dto.*;

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

    private static String sanitizeNumber(String raw) {
        if (raw == null) return "";
        String clean = raw.replaceAll("[^0-9,.-]", "").trim();
        if (clean.contains(",") && clean.contains(".")) {
            if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) {
                clean = clean.replace(".", "").replace(',', '.');
            } else {
                clean = clean.replace(",", "");
            }
        } else {
            clean = clean.replace(',', '.');
        }
        return clean;
    }

    /** Decimal > 0 a partir de texto livre; vazio/inválido/≤0 → null (campos opcionais de grosso). */
    private static BigDecimal parsePositiveOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            BigDecimal v = new BigDecimal(sanitizeNumber(raw));
            return v.signum() > 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Decimal ≥ 0 a partir de texto livre; vazio/inválido → 0 (para unidades soltas). */
    private static BigDecimal parseDecimalOrZero(String raw) {
        if (raw == null || raw.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal v = new BigDecimal(sanitizeNumber(raw));
            return v.signum() < 0 ? BigDecimal.ZERO : v;
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    public void createBatchEntryDialog(ProductDTO preselected) {
        List<ProductDTO> products = new ArrayList<>(owner.catalogProducts);
        if (products.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário", "Registe primeiro um produto.");
            return;
        }
        if (owner.warehousesList.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazém necessário", "Registe primeiro um armazém.");
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
        DateField expirationField = new DateField();
        JTextField batchField = new JTextField();
        JTextField serialField = new JTextField();
        JTextField descField = new JTextField("Entrada de lote/validade");

        UIHelper.styleComboBox(prodCombo);
        UIHelper.styleComboBox(whCombo);
        UIHelper.styleTextField(boxesField);
        UIHelper.styleTextField(looseField);
        UIHelper.styleTextField(totalUnitsField);
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
                owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", "Indique uma quantidade maior que zero em caixas e/ou unidades soltas.");
                return;
            }
        } catch (NumberFormatException ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", "Introduza uma quantidade numérica válida.");
            return;
        }

        String expRaw = expirationField.getText().trim();
        if (expRaw.isEmpty()) {
            owner.showStockNotice(FeedbackType.ERROR, "Validade obrigatória", "Introduza a validade no formato yyyy-MM-dd.");
            return;
        }
        LocalDate expirationDate;
        try {
            expirationDate = LocalDate.parse(expRaw);
        } catch (DateTimeParseException ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Validade inválida", "Use o formato yyyy-MM-dd, por exemplo 2027-12-31.");
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
            owner.showStockSuccess("Lote registado com sucesso para '" + selectedDTO.name() + "'.");
            owner.onPanelSelected();
        }, owner::showStockError);
    }

    public void createProductDialog() {
        loadProductOptions(options -> showCreateProductDialog(options.categories(), options.vatRates()));
    }

    private void showCreateProductDialog(
            java.util.List<mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO> categories,
            java.util.List<mz.multicore.erp.modules.fiscal.dto.TaxRateDTO> vatRates) {
        JTextField skuField = new JTextField();
        JTextField referenceField = new JTextField();
        JTextField barcodeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField salesPriceField = new JTextField();
        JTextField purchasePriceField = new JTextField();
        JTextField minStockField = new JTextField("0");
        JTextField unitsPerBoxField = new JTextField("1");
        JTextField netWeightField = new JTextField();
        JTextField grossWeightField = new JTextField();
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
        UIHelper.styleTextField(netWeightField);
        UIHelper.styleTextField(grossWeightField);
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
        taxCombo.addItem("— IVA Padrão (16%) —");
        int defaultTaxIdx = 0;
        for (int i = 0; i < vatRates.size(); i++) {
            taxCombo.addItem(vatRates.get(i).name());
            if ("IVA_STANDARD".equals(vatRates.get(i).type())) defaultTaxIdx = i + 1;
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
                    owner.showStockNotice(FeedbackType.ERROR, "Imagem inválida", "Não foi possível ler a imagem seleccionada.");
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
                "Peso líquido/unidade (kg):", netWeightField,
                "Peso bruto/unidade (kg):", grossWeightField,
                "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField,
                "Descrição:", descField,
                "Imagem (opcional):", imagePanel
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Registar Novo Produto", "fas-boxes",
                "Defina os dados e o IVA do artigo", dialogPanel)
                .setConfirmButton("Registar Produto", "fas-save");

        final ProductDTO[] createdHolder = {null};
        dialog.setOnSaveAsync(() -> {
            String sku = skuField.getText().trim();
            String reference = referenceField.getText().trim();
            String barcode = barcodeField.getText().trim();
            String name = nameField.getText().trim();
            String salesPriceStr = sanitizeNumber(salesPriceField.getText());
            String purchasePriceStr = sanitizeNumber(purchasePriceField.getText());
            String minStockStr = sanitizeNumber(minStockField.getText());
            String unitsPerBoxStr = unitsPerBoxField.getText().trim();
            String desc = descField.getText().trim();

            if (sku.isEmpty() || name.isEmpty() || salesPriceStr.isEmpty()) {
                throw new BusinessRuleException("Preencha SKU, Nome e Preço de Venda.");
            }

            BigDecimal salesPrice;
            BigDecimal purchasePrice;
            BigDecimal minStock;
            try {
                salesPrice = new BigDecimal(salesPriceStr);
                purchasePrice = purchasePriceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(purchasePriceStr);
                minStock = minStockStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(minStockStr);
            } catch (NumberFormatException nfe) {
                throw new BusinessRuleException("Preços e stock mínimo devem ser números válidos.");
            }

            if (salesPrice.signum() < 0) throw new BusinessRuleException("O preço de venda não pode ser negativo.");
            if (purchasePrice.signum() < 0) throw new BusinessRuleException("O preço de compra não pode ser negativo.");
            if (minStock.signum() < 0) throw new BusinessRuleException("O stock mínimo não pode ser negativo.");

            int unitsPerBox = parseIntOrZero(unitsPerBoxStr);
            if (unitsPerBox < 1) unitsPerBox = 1;

            int catIdx = categoryCombo.getSelectedIndex();
            Long categoryId = null;
            if (catIdx > 0 && (catIdx - 1) < categories.size()) {
                categoryId = categories.get(catIdx - 1).id();
            }

            Long taxRateId = null;
            int taxIdx = taxCombo.getSelectedIndex();
            if (taxIdx > 0 && (taxIdx - 1) < vatRates.size()) {
                taxRateId = vatRates.get(taxIdx - 1).id();
            }

            BigDecimal wholesalePrice = parsePositiveOrNull(wholesalePriceField.getText());
            BigDecimal wholesaleMinQty = parsePositiveOrNull(wholesaleMinQtyField.getText());
            BigDecimal netWeightKg = parsePositiveOrNull(netWeightField.getText());
            BigDecimal grossWeightKg = parsePositiveOrNull(grossWeightField.getText());

            if (netWeightKg != null && grossWeightKg != null && grossWeightKg.compareTo(netWeightKg) < 0) {
                throw new BusinessRuleException("O peso bruto deve ser igual ou superior ao peso líquido.");
            }

            Long selectedCategoryId = categoryId;
            Long selectedTaxRateId = taxRateId;
            int selectedUnitsPerBox = unitsPerBox;
            byte[] selectedImage = imageHolder[0];

            return () -> {
                ProductDTO created = owner.comercialApiClient.createProduct(
                        sku, reference.isEmpty() ? null : reference,
                        barcode.isEmpty() ? null : barcode, name, salesPrice, purchasePrice,
                        minStock, selectedUnitsPerBox, selectedCategoryId, "UNIT", true,
                        selectedTaxRateId, desc.isEmpty() ? null : desc,
                        wholesalePrice, wholesaleMinQty, netWeightKg, grossWeightKg);
                if (selectedImage != null) {
                    owner.comercialApiClient.updateProductImage(created.id(), selectedImage);
                }
                createdHolder[0] = created;
                return created;
            };
        });

        if (dialog.showDialog()) {
            owner.onPanelSelected();
            ProductDTO created = createdHolder[0];
            if (created != null) {
                int addStock = JOptionPane.showConfirmDialog(owner,
                        "Produto '" + created.name() + "' cadastrado.\nDeseja adicionar stock inicial com validade agora?",
                        "Adicionar stock inicial", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (addStock == JOptionPane.YES_OPTION) createBatchEntryDialog(created);
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
                preselectedProductId, options.products(), options.categories(), options.vatRates()));
    }

    private void showEditProductDialog(Long preselectedProductId,
            java.util.List<ProductDTO> products,
            java.util.List<mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO> categories,
            java.util.List<mz.multicore.erp.modules.fiscal.dto.TaxRateDTO> vatRates) {
        if (products == null || products.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário", "Não foram encontrados produtos registados.");
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
        JTextField netWeightField = new JTextField();
        JTextField grossWeightField = new JTextField();
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
        UIHelper.styleTextField(netWeightField);
        UIHelper.styleTextField(grossWeightField);
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
        taxCombo.addItem("— IVA Padrão (16%) —");
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
                    owner.showStockNotice(FeedbackType.ERROR, "Imagem inválida", "Não foi possível ler a imagem seleccionada.");
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
            netWeightField.setText(p.netUnitWeightKg() == null ? "" : p.netUnitWeightKg().toPlainString());
            grossWeightField.setText(p.grossUnitWeightKg() == null ? "" : p.grossUnitWeightKg().toPlainString());
            wholesalePriceField.setText(p.wholesalePrice() == null ? "" : p.wholesalePrice().toPlainString());
            wholesaleMinQtyField.setText(p.wholesaleMinQty() == null ? "" : p.wholesaleMinQty().toPlainString());
            descField.setText(p.description() == null ? "" : p.description());

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
                "Peso líquido/unidade (kg):", netWeightField,
                "Peso bruto/unidade (kg):", grossWeightField,
                "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField,
                "Descrição:", descField,
                "Imagem (opcional):", imagePanel
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Editar Produto", "fas-edit",
                "Actualize os dados do artigo", dialogPanel)
                .setConfirmButton("Actualizar", "fas-save");

        dialog.setOnSaveAsync(() -> {
            int idx = productCombo.getSelectedIndex();
            if (idx < 0 || idx >= products.size()) {
                throw new BusinessRuleException("Selecione um produto válido.");
            }
            ProductDTO selected = products.get(idx);

            String reference = referenceField.getText().trim();
            String barcode = barcodeField.getText().trim();
            String name = nameField.getText().trim();
            String salesPriceStr = sanitizeNumber(salesPriceField.getText());
            String purchasePriceStr = sanitizeNumber(purchasePriceField.getText());
            String minStockStr = sanitizeNumber(minStockField.getText());
            String unitsPerBoxStr = unitsPerBoxField.getText().trim();
            String desc = descField.getText().trim();

            if (name.isEmpty() || salesPriceStr.isEmpty()) {
                throw new BusinessRuleException("Preencha Nome e Preço de Venda.");
            }

            BigDecimal salesPrice;
            BigDecimal purchasePrice;
            BigDecimal minStock;
            try {
                salesPrice = new BigDecimal(salesPriceStr);
                purchasePrice = purchasePriceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(purchasePriceStr);
                minStock = minStockStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(minStockStr);
            } catch (NumberFormatException nfe) {
                throw new BusinessRuleException("Preços e stock mínimo devem ser números válidos.");
            }

            if (salesPrice.signum() < 0) throw new BusinessRuleException("O preço de venda não pode ser negativo.");
            if (purchasePrice.signum() < 0) throw new BusinessRuleException("O preço de compra não pode ser negativo.");
            if (minStock.signum() < 0) throw new BusinessRuleException("O stock mínimo não pode ser negativo.");

            int unitsPerBox = parseIntOrZero(unitsPerBoxStr);
            if (unitsPerBox < 1) unitsPerBox = 1;

            int catIdx = categoryCombo.getSelectedIndex();
            Long categoryId = null;
            if (catIdx > 0 && (catIdx - 1) < categories.size()) {
                categoryId = categories.get(catIdx - 1).id();
            }

            Long taxRateId = null;
            int taxIdx = taxCombo.getSelectedIndex();
            if (taxIdx > 0 && (taxIdx - 1) < vatRates.size()) {
                taxRateId = vatRates.get(taxIdx - 1).id();
            }

            BigDecimal wholesalePrice = parsePositiveOrNull(wholesalePriceField.getText());
            BigDecimal wholesaleMinQty = parsePositiveOrNull(wholesaleMinQtyField.getText());
            BigDecimal netWeightKg = parsePositiveOrNull(netWeightField.getText());
            BigDecimal grossWeightKg = parsePositiveOrNull(grossWeightField.getText());

            if (netWeightKg != null && grossWeightKg != null && grossWeightKg.compareTo(netWeightKg) < 0) {
                throw new BusinessRuleException("O peso bruto deve ser igual ou superior ao peso líquido.");
            }

            Long selectedCategoryId = categoryId;
            Long selectedTaxRateId = taxRateId;
            int selectedUnitsPerBox = unitsPerBox;
            byte[] selectedImage = imageHolder[0];
            String saleType = selected.saleType() != null ? selected.saleType() : "UNIT";
            boolean stockTracked = selected.stockTracked();

            return () -> {
                owner.comercialApiClient.updateProduct(
                        selected.id(), reference.isEmpty() ? null : reference,
                        barcode.isEmpty() ? null : barcode, name, salesPrice, purchasePrice,
                        minStock, selectedUnitsPerBox, selectedCategoryId, saleType,
                        stockTracked, selectedTaxRateId,
                        desc.isEmpty() ? null : desc, wholesalePrice, wholesaleMinQty,
                        netWeightKg, grossWeightKg);
                if (selectedImage != null) {
                    owner.comercialApiClient.updateProductImage(selected.id(), selectedImage);
                }
                return null;
            };
        });

        boolean confirmed = dialog.showDialog();
        if (confirmed) {
            owner.onPanelSelected();
            owner.showStockSuccess("Produto actualizado com sucesso.");
        }
    }

    private void loadProductOptions(java.util.function.Consumer<ProductOptions> onLoaded) {
        UIHelper.loadAsync(owner, () -> {
            java.util.List<ProductDTO> products = owner.comercialApiClient.getAllProducts();
            owner.catalogProducts = products;
            return new ProductOptions(
                    products,
                    owner.comercialApiClient.getActiveCategories(),
                    owner.comercialApiClient.getActiveVatRates());
        }, onLoaded, owner::showStockError);
    }

    private record ProductOptions(
            java.util.List<ProductDTO> products,
            java.util.List<mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO> categories,
            java.util.List<mz.multicore.erp.modules.fiscal.dto.TaxRateDTO> vatRates) {}
}

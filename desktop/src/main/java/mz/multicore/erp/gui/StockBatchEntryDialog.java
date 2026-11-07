package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.RegisterMovementRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo modal corporativo para registo de entrada de lote e validade (FEFO) de produto.
 */
final class StockBatchEntryDialog {

    private StockBatchEntryDialog() {}

    static void show(StockPanel owner, ProductDTO preselected) {
        List<ProductDTO> products = new ArrayList<>(owner.catalogProducts);
        if (products.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário", "Registe primeiro um produto.");
            return;
        }
        if (owner.warehousesList.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazém necessário", "Registe primeiro um armazém.");
            return;
        }

        ProductSearchComboBox prodCombo = new ProductSearchComboBox();
        JComboBox<String> whCombo = new JComboBox<>();
        JTextField boxesField = new JTextField("0");
        JTextField packagesField = new JTextField("0");
        JTextField looseField = new JTextField("0");
        JTextField totalUnitsField = new JTextField();
        totalUnitsField.setEditable(false);
        JLabel unitsPerBoxHint = new JLabel(" ");
        unitsPerBoxHint.setForeground(UIHelper.TEXT_MUTED);
        DateField expirationField = new DateField();
        JTextField batchField = new JTextField();
        JTextField serialField = new JTextField();
        JTextField descField = new JTextField("Entrada de lote/validade");

        UIHelper.styleComboBox(whCombo);
        for (JTextField f : List.of(boxesField, packagesField, looseField, totalUnitsField, batchField, serialField, descField)) {
            UIHelper.styleTextField(f);
        }

        expirationField.putClientProperty("JTextField.placeholderText", "Opcional - deixe vazio se o produto não tem validade");
        batchField.putClientProperty("JTextField.placeholderText", "Opcional - gerado a partir da validade se vazio");
        serialField.putClientProperty("JTextField.placeholderText", "Opcional");

        prodCombo.setProducts(products);
        for (WarehouseDTO w : owner.warehousesList) whCombo.addItem(w.name());
        if (preselected != null) prodCombo.selectProduct(preselected.id());

        Runnable recomputeTotal = () -> {
            ProductDTO product = prodCombo.selectedProduct();
            int packagesPerBox = product == null ? 1 : Math.max(1, product.packagesPerBox());
            int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
            int unitsPerBox = product == null ? 1 : Math.max(1, product.unitsPerBox());
            unitsPerBoxHint.setText(packagesPerBox + " emb/cx × " + unitsPerPackage + " un/emb = " + unitsPerBox + " un/cx");
            int boxes = parseIntOrZero(boxesField.getText());
            int packages = parseIntOrZero(packagesField.getText());
            BigDecimal loose = parseDecimalOrZero(looseField.getText());
            BigDecimal total = BigDecimal.valueOf((long) boxes * unitsPerBox)
                    .add(BigDecimal.valueOf((long) packages * unitsPerPackage))
                    .add(loose);
            totalUnitsField.setText(total.stripTrailingZeros().toPlainString());
        };
        prodCombo.addActionListener(e -> recomputeTotal.run());
        UIHelper.onTextChange(boxesField, recomputeTotal);
        UIHelper.onTextChange(packagesField, recomputeTotal);
        UIHelper.onTextChange(looseField, recomputeTotal);
        recomputeTotal.run();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Produto:", prodCombo,
                "Armazém:", whCombo,
                "Nº de Caixas:", boxesField,
                "Embalagens soltas:", packagesField,
                "Unidades soltas:", looseField,
                "Composição da caixa:", unitsPerBoxHint,
                "Total (unidades):", totalUnitsField,
                "Validade (yyyy-MM-dd):", expirationField,
                "Nº Lote:", batchField,
                "Nº Série:", serialField,
                "Descrição:", descField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Adicionar Lote / Validade", "fas-boxes",
                "Registe a entrada; a validade só é necessária para perecíveis (FEFO)", dialogPanel).showDialog();
        if (!confirmed) return;

        int whIdx = whCombo.getSelectedIndex();
        ProductDTO selectedDTO = prodCombo.selectedProduct();
        if (selectedDTO == null || whIdx < 0) return;

        int loosePackages = parseIntOrZero(packagesField.getText());
        BigDecimal looseUnits = parseDecimalOrZero(looseField.getText());
        if (loosePackages >= selectedDTO.packagesPerBox()) {
            owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", "As embalagens soltas devem ser inferiores a uma caixa.");
            return;
        }
        if (looseUnits.compareTo(BigDecimal.valueOf(selectedDTO.unitsPerPackage())) >= 0) {
            owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", "As unidades soltas devem ser inferiores a uma embalagem.");
            return;
        }

        BigDecimal qty;
        try {
            qty = new BigDecimal(totalUnitsField.getText().trim());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", "Indique uma quantidade maior que zero em caixas, embalagens e/ou unidades.");
                return;
            }
        } catch (NumberFormatException ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", "Introduza uma quantidade numérica válida.");
            return;
        }

        // Validade só obrigatória em produtos perecíveis; loiça, mobília e afins entram sem validade.
        String expRaw = expirationField.getText().trim();
        LocalDate expirationDate = null;
        if (expRaw.isEmpty() && selectedDTO.tracksExpiry()) {
            owner.showStockNotice(FeedbackType.ERROR, "Validade obrigatória",
                    "'" + selectedDTO.name() + "' é perecível. Introduza a validade no formato yyyy-MM-dd.");
            return;
        }
        if (!expRaw.isEmpty()) {
            try {
                expirationDate = LocalDate.parse(expRaw);
            } catch (DateTimeParseException ex) {
                owner.showStockNotice(FeedbackType.ERROR, "Validade inválida", "Use o formato yyyy-MM-dd, por exemplo 2027-12-31.");
                return;
            }
        }
        // O servidor recusa lotes vencidos (ProductBatchMutationService); avisar já aqui.
        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            owner.showStockNotice(FeedbackType.ERROR, "Validade expirada",
                    "Não é possível dar entrada de um lote já vencido. Verifique a data de validade.");
            return;
        }

        String batch = batchField.getText().trim();
        if (batch.isEmpty()) batch = null;
        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;
        String desc = descField.getText().trim();
        WarehouseDTO selectedWarehouse = owner.warehousesList.get(whIdx);

        RegisterMovementRequest request = new RegisterMovementRequest(
                selectedDTO.id(), selectedWarehouse.id(), qty, "ENTRY",
                batch, serial, desc, expirationDate, "MOVEMENT-" + java.util.UUID.randomUUID());
        UIHelper.runWithProgress(owner, "A registar entrada de lote…",
                () -> owner.inventoryApiClient.registerMovement(request), ignored -> {
                    owner.showStockSuccess("Lote registado com sucesso para '" + selectedDTO.name() + "'.");
                    owner.onPanelSelected();
                }, owner::showStockError);
    }

    private static int parseIntOrZero(String raw) {
        if (raw == null) return 0;
        try { return Math.max(0, Integer.parseInt(raw.trim())); } catch (NumberFormatException ex) { return 0; }
    }

    private static BigDecimal parseDecimalOrZero(String raw) {
        if (raw == null || raw.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal v = new BigDecimal(raw.replaceAll("[^0-9,.-]", "").replace(',', '.').trim());
            return v.signum() < 0 ? BigDecimal.ZERO : v;
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}

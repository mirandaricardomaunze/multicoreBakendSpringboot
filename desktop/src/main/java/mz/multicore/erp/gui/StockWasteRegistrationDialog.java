package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.CreateStockWasteRequest;
import mz.multicore.erp.modules.inventory.dto.ExpiringBatchAlertDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo modal para registo ergonómico de quebras de stock e abates de mercadoria.
 */
public final class StockWasteRegistrationDialog {

    private record WarehouseItem(Long id, String name) {
        @Override public String toString() { return name; }
    }

    private record ProductItem(Long id, String sku, String name, BigDecimal cost) {
        @Override public String toString() { return sku + " - " + name; }
    }

    private StockWasteRegistrationDialog() {}

    public static void open(
            Component parent, StockWasteApiClient wasteApiClient, List<WarehouseDTO> cachedWarehouses,
            List<ProductDTO> cachedProducts, ExpiringBatchAlertDTO prefillBatch, Runnable onSuccess
    ) {
        if (cachedWarehouses.isEmpty() || cachedProducts.isEmpty()) {
            ToastManager.show(parent, FeedbackType.INFO, "A carregar dados de apoio, aguarde um momento...");
            return;
        }

        WarehouseItem[] warehouseItems = cachedWarehouses.stream()
                .map(w -> new WarehouseItem(w.id(), w.name()))
                .toArray(WarehouseItem[]::new);
        JComboBox<WarehouseItem> whCombo = UIHelper.createComboBox(warehouseItems, WarehouseItem::name);
        whCombo.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));

        SearchableComboBox<ProductItem> prdCombo = new SearchableComboBox<>(
                "Pesquisar produto por código ou nome", ProductItem::toString, p -> p.sku() + " " + p.name());
        prdCombo.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));
        List<ProductItem> productItems = new ArrayList<>();
        for (ProductDTO p : cachedProducts) {
            productItems.add(new ProductItem(p.id(), p.sku(), p.name(), p.purchasePrice() != null ? p.purchasePrice() : p.unitPrice()));
        }
        prdCombo.setItems(productItems);

        JTextField qtyField = new JTextField("1");
        UIHelper.styleTextField(qtyField);
        qtyField.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<WasteReason> reasonCombo = UIHelper.createComboBox(
                WasteReason.values(), WasteReason::getDescription);
        reasonCombo.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));

        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);
        notesField.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));

        JLabel costPreviewLabel = new JLabel("Custo Estimado: 0,00 MT");
        costPreviewLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        costPreviewLabel.setForeground(UIHelper.ACCENT_BLUE);

        JLabel thresholdWarning = new JLabel("Perdas > 2.500,00 MT requerem aprovação formal da gerência.");
        thresholdWarning.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        thresholdWarning.setForeground(UIHelper.PENDING_YELLOW);

        Runnable updateCostPreview = () -> {
            ProductItem selProd = (ProductItem) prdCombo.getSelectedItem();
            if (selProd == null) return;
            try {
                BigDecimal qty = new BigDecimal(qtyField.getText().trim().replace(",", "."));
                BigDecimal cost = selProd.cost().multiply(qty).setScale(2, RoundingMode.HALF_UP);
                costPreviewLabel.setText("Custo Estimado: " + UIHelper.formatMzn(cost));
                boolean overThreshold = cost.compareTo(new BigDecimal("2500.00")) > 0;
                thresholdWarning.setText(overThreshold ? "Atenção: Custo excede 2.500 MT. Ficará pendente de aprovação."
                        : "Custo dentro da alçada ordinária (auto-aprovação).");
                thresholdWarning.setForeground(overThreshold ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
            } catch (Exception ex) {
                costPreviewLabel.setText("Custo Estimado: 0,00 MT");
            }
        };

        prdCombo.addActionListener(e -> updateCostPreview.run());
        UIHelper.onTextChange(qtyField, updateCostPreview);

        Long batchId = null;
        if (prefillBatch != null) {
            batchId = prefillBatch.batchId();
            for (int i = 0; i < whCombo.getItemCount(); i++) {
                if (whCombo.getItemAt(i).id().equals(prefillBatch.warehouseId())) { whCombo.setSelectedIndex(i); break; }
            }
            for (int i = 0; i < prdCombo.getItemCount(); i++) {
                if (prdCombo.getItemAt(i).id().equals(prefillBatch.productId())) { prdCombo.setSelectedIndex(i); break; }
            }
            qtyField.setText(prefillBatch.quantity().stripTrailingZeros().toPlainString());
            reasonCombo.setSelectedItem(WasteReason.EXPIRED);
            notesField.setText("Quebra por validade expirada / crítica (Lote: " + prefillBatch.batchNumber() + ")");
        }
        updateCostPreview.run();

        JPanel formPanel = UIHelper.createDialogForm("Armazém:", whCombo, "Produto:", prdCombo,
                "Quantidade:", qtyField, "Motivo:", reasonCombo, "Observações:", notesField);

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        wrapper.add(formPanel, BorderLayout.NORTH);

        JPanel infoBottom = new JPanel(new GridLayout(2, 1, 0, 4));
        infoBottom.setOpaque(false);
        infoBottom.add(costPreviewLabel);
        infoBottom.add(thresholdWarning);
        wrapper.add(infoBottom, BorderLayout.SOUTH);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Registar Quebra de Stock",
                "fas-trash-alt", "Abate de mercadoria danificada, vencida ou avariada", wrapper);

        final BigDecimal[] validQty = new BigDecimal[1];
        dlg.setOnSave(() -> {
            try {
                BigDecimal q = new BigDecimal(qtyField.getText().trim().replace(",", "."));
                if (q.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessRuleException("A quantidade deve ser superior a zero.");
                validQty[0] = q;
            } catch (NumberFormatException ex) {
                throw new BusinessRuleException("Quantidade inválida. Digite um valor numérico positivo.");
            }
        });

        if (!dlg.showDialog()) return;

        WarehouseItem selWh = (WarehouseItem) whCombo.getSelectedItem();
        ProductItem selPrd = (ProductItem) prdCombo.getSelectedItem();
        WasteReason selReason = (WasteReason) reasonCombo.getSelectedItem();
        final Long finalBatchId = batchId;

        CreateStockWasteRequest req = new CreateStockWasteRequest(CurrentUserContext.requireCurrentCompanyId(),
                selWh.id(), selPrd.id(), finalBatchId, validQty[0], selReason, notesField.getText().trim(),
                "WASTE-" + java.util.UUID.randomUUID());

        UIHelper.runWithProgress(parent, "A registar quebra de stock...", () -> wasteApiClient.register(req), res -> {
            if (res.status() == WasteStatus.APPROVED) {
                ToastManager.success(parent, "Quebra registada e stock abatido com sucesso!");
            } else {
                ToastManager.show(parent, FeedbackType.INFO, "Registo guardado com estado 'Pendente Aprovação' por exceder a alçada.");
            }
            if (onSuccess != null) onSuccess.run();
        }, err -> ToastManager.show(parent, FeedbackType.ERROR, "Erro ao registar: " + err.getMessage()));
    }
}

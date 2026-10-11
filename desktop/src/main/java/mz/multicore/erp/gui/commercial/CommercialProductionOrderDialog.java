package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.dto.CreateProductionOrderRequest;
import mz.multicore.erp.modules.manufacturing.dto.ProductionOrderDTO;

import javax.swing.*;
import java.awt.Component;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo interativo para converter itens de Cotações ou Encomendas diretamente em Ordens de Fabrico (OP).
 */
public final class CommercialProductionOrderDialog {

    public record DocumentItem(Long productId, String productName, BigDecimal quantity) {}

    private CommercialProductionOrderDialog() {}

    public static void open(
            Component parent,
            ProductionApiClient productionApiClient,
            String documentTitle,
            List<DocumentItem> items,
            List<WarehouseDTO> warehouses,
            Runnable onSuccess
    ) {
        if (productionApiClient == null) {
            ToastManager.show(parent, FeedbackType.INFO,
                    "O módulo de Produção não está inicializado nesta sessão.");
            return;
        }

        if (items == null || items.isEmpty()) {
            ToastManager.show(parent, FeedbackType.WARNING,
                    "O documento não possui itens para envio à produção.");
            return;
        }

        UIHelper.runWithProgress(parent, "A verificar fichas técnicas na Manufatura...", () -> {
            return productionApiClient.listBoms();
        }, boms -> {
            List<BomMatch> matches = new ArrayList<>();
            for (DocumentItem item : items) {
                if (item.productId() == null) continue;
                for (BillOfMaterialsDTO bom : boms) {
                    if (item.productId().equals(bom.finishedProductId())) {
                        matches.add(new BomMatch(bom, item));
                    }
                }
            }

            if (matches.isEmpty()) {
                ToastManager.show(parent, FeedbackType.WARNING,
                        "Nenhum produto de " + documentTitle + " possui ficha técnica registada no módulo de Manufatura.");
                return;
            }

            showDialog(parent, productionApiClient, documentTitle, matches, warehouses, onSuccess);
        }, error -> {
            ToastManager.show(parent, FeedbackType.ERROR,
                    "Erro ao consultar fichas técnicas: " + (error == null ? "Falha na comunicação." : error.getMessage()));
        });
    }

    private static void showDialog(
            Component parent,
            ProductionApiClient apiClient,
            String documentTitle,
            List<BomMatch> matches,
            List<WarehouseDTO> warehouses,
            Runnable onSuccess
    ) {
        JComboBox<BomMatch> bomCombo = new JComboBox<>();
        UIHelper.styleComboBox(bomCombo);
        matches.forEach(bomCombo::addItem);
        bomCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BomMatch m) {
                    setText("[" + m.bom.bomCode() + "] " + m.bom.finishedProductName() + " (Qtd: " + m.item.quantity() + ")");
                }
                return this;
            }
        });

        QuantityField qtyField = new QuantityField("1", false);
        UIHelper.styleTextField(qtyField);
        if (!matches.isEmpty()) {
            qtyField.setText(matches.get(0).item.quantity().toPlainString());
        }

        bomCombo.addActionListener(e -> {
            if (bomCombo.getSelectedItem() instanceof BomMatch m) {
                qtyField.setText(m.item.quantity().toPlainString());
            }
        });

        JComboBox<WarehouseDTO> sourceWhCombo = new JComboBox<>();
        UIHelper.styleComboBox(sourceWhCombo);
        JComboBox<WarehouseDTO> targetWhCombo = new JComboBox<>();
        UIHelper.styleComboBox(targetWhCombo);

        if (warehouses != null) {
            warehouses.forEach(w -> {
                sourceWhCombo.addItem(w);
                targetWhCombo.addItem(w);
            });
        }

        sourceWhCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof WarehouseDTO w) {
                    String num = w.warehouseNumber() != null ? " (" + w.warehouseNumber() + ")" : "";
                    setText(w.name() + num);
                }
                return this;
            }
        });

        targetWhCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof WarehouseDTO w) {
                    String num = w.warehouseNumber() != null ? " (" + w.warehouseNumber() + ")" : "";
                    setText(w.name() + num);
                }
                return this;
            }
        });

        DateField endDateField = new DateField();
        endDateField.setDate(LocalDate.now().plusDays(7));

        JPanel form = UIHelper.createDialogForm(
                "Ficha Técnica (BOM):*", bomCombo,
                "Quantidade a Produzir:*", qtyField,
                "Armazém Matéria-Prima:*", sourceWhCombo,
                "Armazém Produto Acabado:*", targetWhCombo,
                "Conclusão Prevista:*", endDateField
        );

        Window window = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        ModernFormDialog dialog = new ModernFormDialog(
                window,
                "Lançar Ordem de Produção — " + documentTitle,
                "fas-industry",
                "Gere a Ordem de Fabrico a partir dos itens deste documento com base na ficha técnica (BOM).",
                form
        );
        dialog.setConfirmButton("Gerar Ordem de Fabrico", "fas-industry");

        dialog.setOnSaveAsync(() -> {
            BomMatch selectedMatch = (BomMatch) bomCombo.getSelectedItem();
            if (selectedMatch == null) {
                throw new IllegalArgumentException("Selecione a ficha técnica a produzir.");
            }

            BigDecimal qty = qtyField.value();
            if (qty == null || qty.signum() <= 0) {
                qtyField.requestFocusInWindow();
                throw new IllegalArgumentException("A quantidade a produzir deve ser superior a zero.");
            }

            WarehouseDTO sourceWh = (WarehouseDTO) sourceWhCombo.getSelectedItem();
            WarehouseDTO targetWh = (WarehouseDTO) targetWhCombo.getSelectedItem();
            if (sourceWh == null || targetWh == null) {
                throw new IllegalArgumentException("Selecione os armazéns de matéria-prima e produto acabado.");
            }

            LocalDate endDate = endDateField.getDate();
            CreateProductionOrderRequest request = new CreateProductionOrderRequest(
                    selectedMatch.bom.id(),
                    qty,
                    sourceWh.id(),
                    targetWh.id(),
                    endDate
            );

            return () -> {
                ProductionOrderDTO created = apiClient.createOrder(request);
                SwingUtilities.invokeLater(() -> {
                    ToastManager.show(parent, FeedbackType.SUCCESS,
                            "Ordem de Produção " + created.orderNumber() + " lançada na Manufatura!");
                    if (onSuccess != null) onSuccess.run();
                });
                return created;
            };
        });

        dialog.showDialog();
    }

    private record BomMatch(BillOfMaterialsDTO bom, DocumentItem item) {}
}

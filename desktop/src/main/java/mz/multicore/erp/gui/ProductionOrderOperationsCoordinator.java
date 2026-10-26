package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.dto.CreateProductionOrderRequest;
import mz.multicore.erp.modules.manufacturing.dto.ProductionOrderDTO;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Coordenador de operações do ciclo de vida de Ordens de Produção (OPs).
 */
public final class ProductionOrderOperationsCoordinator {

    private final ProductionApiClient productionApiClient;
    private final ComercialApiClient comercialApiClient;
    private final InventoryApiClient inventoryApiClient;

    public ProductionOrderOperationsCoordinator(ProductionApiClient productionApiClient,
                                                ComercialApiClient comercialApiClient,
                                                InventoryApiClient inventoryApiClient) {
        this.productionApiClient = productionApiClient;
        this.comercialApiClient = comercialApiClient;
        this.inventoryApiClient = inventoryApiClient;
    }

    public void openNewOrderDialog(Window parent,
                                   List<BillOfMaterialsDTO> boms,
                                   List<WarehouseDTO> warehouses,
                                   InlineFeedbackPanel feedback,
                                   Runnable onSuccess) {
        if (boms.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Crie primeiro pelo menos uma Ficha Técnica (BOM) antes de lançar ordens de produção.");
            return;
        }

        JComboBox<BillOfMaterialsDTO> bomCombo = new JComboBox<>(boms.toArray(new BillOfMaterialsDTO[0]));
        UIHelper.styleComboBox(bomCombo);
        bomCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BillOfMaterialsDTO b) {
                    setText(b.bomCode() + " - " + b.finishedProductName() + " (" + b.name() + ")");
                }
                return this;
            }
        });

        JTextField qtyField = new JTextField("1.0", 10);
        JComboBox<WarehouseDTO> sourceWhCombo = new JComboBox<>(warehouses.toArray(new WarehouseDTO[0]));
        JComboBox<WarehouseDTO> targetWhCombo = new JComboBox<>(warehouses.toArray(new WarehouseDTO[0]));
        UIHelper.styleComboBox(sourceWhCombo);
        UIHelper.styleComboBox(targetWhCombo);

        DefaultListCellRenderer whRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof WarehouseDTO w) {
                    setText(w.name() + (w.warehouseNumber() != null ? " (" + w.warehouseNumber() + ")" : ""));
                }
                return this;
            }
        };
        sourceWhCombo.setRenderer(whRenderer);
        targetWhCombo.setRenderer(whRenderer);

        DateField endDateField = new DateField();
        endDateField.setText(LocalDate.now().plusDays(7).toString());

        JPanel form = UIHelper.createDialogForm(
                "Ficha Técnica (BOM) *:", bomCombo,
                "Quantidade a Produzir *:", qtyField,
                "Armazém Matéria-Prima *:", sourceWhCombo,
                "Armazém Produto Acabado *:", targetWhCombo,
                "Data Limite Prevista:", endDateField
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Lançar Nova Ordem de Produção", "fas-industry",
                "Emissão de ordem de fabrico com apuramento de componentes", form);

        dialog.setOnSave(() -> {
            BillOfMaterialsDTO selectedBom = (BillOfMaterialsDTO) bomCombo.getSelectedItem();
            if (selectedBom == null) throw new IllegalArgumentException("Selecione a ficha técnica.");

            BigDecimal qty;
            try {
                qty = new BigDecimal(qtyField.getText().trim().replace(",", "."));
                if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                qtyField.requestFocus();
                throw new IllegalArgumentException("A quantidade a produzir deve ser maior que zero.");
            }

            WarehouseDTO srcWh = (WarehouseDTO) sourceWhCombo.getSelectedItem();
            WarehouseDTO tgtWh = (WarehouseDTO) targetWhCombo.getSelectedItem();
            if (srcWh == null || tgtWh == null) {
                throw new IllegalArgumentException("Selecione os armazéns de origem e destino.");
            }

            LocalDate endDate = null;
            if (!endDateField.getText().trim().isEmpty()) {
                try {
                    endDate = LocalDate.parse(endDateField.getText().trim());
                } catch (Exception ex) {
                    endDateField.requestFocus();
                    throw new IllegalArgumentException("Data inválida. Use AAAA-MM-DD.");
                }
            }

            CreateProductionOrderRequest req = new CreateProductionOrderRequest(
                    selectedBom.id(),
                    qty,
                    srcWh.id(),
                    tgtWh.id(),
                    endDate
            );

            productionApiClient.createOrder(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Ordem de produção criada com sucesso.");
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void showOrderManagementDialog(Window parent,
                                           ProductionOrderDTO order,
                                           List<ProductDTO> cachedProducts,
                                           InlineFeedbackPanel feedback,
                                           Runnable onSuccess) {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        ModernButton startBtn = UIHelper.createPrimaryButton("Iniciar Produção (Passar a EM_PRODUÇÃO)");
        startBtn.setIcon(UIHelper.icon("fas-play", 14));
        startBtn.setEnabled(order.status() == ProductionOrderStatus.PLANNED);

        ModernButton completeBtn = UIHelper.createSuccessButton("Concluir Produção & Dar Entrada no Stock");
        completeBtn.setIcon(UIHelper.icon("fas-check-double", 14));
        completeBtn.setEnabled(order.status() == ProductionOrderStatus.IN_PROGRESS || order.status() == ProductionOrderStatus.PLANNED);

        ModernButton labelsBtn = UIHelper.createSecondaryButton("Imprimir Etiquetas do Lote / OP");
        labelsBtn.setIcon(UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT_BLUE));

        ModernButton cancelBtn = UIHelper.createSecondaryButton("Cancelar Ordem");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 14));
        cancelBtn.setEnabled(order.status() != ProductionOrderStatus.COMPLETED && order.status() != ProductionOrderStatus.CANCELLED);

        panel.add(startBtn);
        panel.add(completeBtn);
        panel.add(labelsBtn);
        panel.add(cancelBtn);

        ModernFormDialog dialog = new ModernFormDialog(parent, "Gerir Ordem " + order.orderNumber(), "fas-cogs",
                "Estado actual: " + order.status().name(), panel);

        labelsBtn.addActionListener(e -> {
            dialog.close();
            printOrderLabels(parent, order, cachedProducts);
        });

        startBtn.addActionListener(e -> {
            try {
                productionApiClient.startOrder(order.id());
                feedback.show(FeedbackType.SUCCESS, "Ordem " + order.orderNumber() + " iniciada com sucesso.");
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                feedback.show(FeedbackType.ERROR, "Erro: " + ex.getMessage());
            }
        });

        completeBtn.addActionListener(e -> {
            try {
                productionApiClient.completeOrder(order.id(), order.plannedQuantity());
                feedback.show(FeedbackType.SUCCESS, "Ordem " + order.orderNumber() + " concluída! Stock do produto acabado e CIP apurados.");
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                feedback.show(FeedbackType.ERROR, "Erro: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> {
            try {
                productionApiClient.cancelOrder(order.id());
                feedback.show(FeedbackType.SUCCESS, "Ordem " + order.orderNumber() + " cancelada.");
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                feedback.show(FeedbackType.ERROR, "Erro: " + ex.getMessage());
            }
        });

        dialog.showDialog();
    }

    public void openProductionLabels(Window parent, List<ProductDTO> cachedProducts) {
        List<ProductDTO> products = !cachedProducts.isEmpty() ? cachedProducts :
                (comercialApiClient != null ? comercialApiClient.getAllProducts() : List.of());
        ShelfLabelsDialog.show(parent, products);
    }

    public void printOrderLabels(Window parent, ProductionOrderDTO order, List<ProductDTO> cachedProducts) {
        ProductDTO finishedProd = cachedProducts.stream()
                .filter(p -> p.name().equalsIgnoreCase(order.finishedProductName()) || p.sku().equalsIgnoreCase(order.bomCode()))
                .findFirst()
                .orElse(null);

        if (finishedProd == null && comercialApiClient != null) {
            try {
                finishedProd = comercialApiClient.getAllProducts().stream()
                        .filter(p -> p.name().equalsIgnoreCase(order.finishedProductName()))
                        .findFirst()
                        .orElse(null);
            } catch (Exception ignored) {}
        }

        List<ProductDTO> list = finishedProd != null ? List.of(finishedProd) : cachedProducts;
        int copies = 1;
        if (order.producedQuantity() != null && order.producedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            copies = order.producedQuantity().intValue();
        } else if (order.plannedQuantity() != null && order.plannedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            copies = order.plannedQuantity().intValue();
        }

        ShelfLabelsDialog.showWithBatch(parent, list, order.orderNumber(), Math.max(1, copies));
    }

    public void openBarcodeCollector(Component parent, Runnable onSuccess) {
        var collectorDialog = new BarcodeScannerCollectorDialog(
                SwingUtilities.getWindowAncestor(parent),
                BarcodeScannerCollectorDialog.CollectorMode.MANUFACTURING,
                comercialApiClient,
                productionApiClient,
                inventoryApiClient,
                onSuccess
        );
        collectorDialog.showDialog();
    }
}

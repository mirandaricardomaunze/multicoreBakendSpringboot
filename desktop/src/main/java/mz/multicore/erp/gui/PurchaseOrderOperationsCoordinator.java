package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.purchases.dto.*;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Coordenador de operações do ciclo de vida de encomendas a fornecedor. */
final class PurchaseOrderOperationsCoordinator {

    private final ComprasPanel owner;
    private final PurchaseOrdersPanel panel;

    PurchaseOrderOperationsCoordinator(ComprasPanel owner, PurchaseOrdersPanel panel) {
        this.owner = owner;
        this.panel = panel;
    }

    void applyPurchaseOrders(List<PurchaseOrderDTO> loaded) {
        owner.poList = loaded;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        panel.getPoListModel().setRowCount(0);
        java.util.Set<String> bus = new java.util.TreeSet<>();
        for (PurchaseOrderDTO o : owner.poList) {
            String buName = o.businessUnitName() != null && !o.businessUnitName().isBlank() ? o.businessUnitName() : "—";
            if (!"—".equals(buName)) bus.add(buName);
            panel.getPoListModel().addRow(new Object[]{
                    o.orderNumber(), o.supplierName(), o.status(),
                    o.totalAmount() == null ? BigDecimal.ZERO : o.totalAmount(),
                    o.orderDate() == null ? "-" : o.orderDate().format(dtf),
                    o.expectedDate() == null ? "-" : o.expectedDate().toString(),
                    buName});
        }
        if (owner.poBusinessUnitCombo != null) {
            Object prev = owner.poBusinessUnitCombo.getSelectedItem();
            owner.poBusinessUnitCombo.removeAllItems();
            owner.poBusinessUnitCombo.addItem("Todos os negócios");
            owner.poBusinessUnitCombo.addItem("—");
            for (String b : bus) owner.poBusinessUnitCombo.addItem(b);
            if (prev != null) owner.poBusinessUnitCombo.setSelectedItem(prev);
        }
    }

    void openSelectedEditor() {
        PurchaseOrderDTO selected = panel.selectedPO();
        if (selected == null) return;
        panel.setEditingOrder(selected);
        boolean editable = "ORDERED".equals(selected.status())
                && selected.lines().stream().noneMatch(line -> line.receivedQuantity() != null
                && line.receivedQuantity().signum() > 0);
        panel.setLineGridEditable(editable);
        panel.setEditorDirty(false);
        selectSupplier(selected.supplierId());
        selectWarehouse(selected.warehouseId());
        panel.getPoExpectedField().setText(selected.expectedDate() == null ? "" : selected.expectedDate().toString());
        owner.poDraftLines.clear();
        if (selected.lines() != null) {
            for (PurchaseOrderLineDTO line : selected.lines()) {
                owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                        line.productId(), line.quantity(), line.unitPrice(), line.batchNumber(),
                        line.expirationDate(), line.serialNumber()));
            }
        }
        panel.configureProductCellEditor();
        panel.refreshPoRows();
        panel.setEditorDirty(false);
        panel.getEditorHost().setEditorTitle((editable ? "Editar Encomenda " : "Consultar Encomenda ")
                + selected.orderNumber());
        panel.getEditorHost().setSaveText("Guardar alterações");
        panel.getEditorHost().setSaveEnabled(editable);
        panel.showEditorPage();
    }

    void executeSave(CreatePurchaseOrderRequest create, PurchaseOrderDTO editingOrder) {
        if (editingOrder == null) {
            UIHelper.runWithProgress(owner, "A emitir encomenda...",
                    () -> owner.purchaseApiClient.createOrder(create),
                    saved -> finishPurchaseOrderSave(saved, "criada"), owner::showPurchaseError);
        } else {
            UpdatePurchaseOrderRequest request = new UpdatePurchaseOrderRequest(
                    editingOrder.version(), create.supplierId(), create.warehouseId(),
                    create.expectedDate(), create.notes(), create.lines());
            UIHelper.runWithProgress(owner, "A guardar alterações...",
                    () -> owner.purchaseApiClient.updateOrder(editingOrder.id(), request),
                    saved -> finishPurchaseOrderSave(saved, "actualizada"), owner::showPurchaseError);
        }
    }

    private void finishPurchaseOrderSave(PurchaseOrderDTO saved, String action) {
        panel.setEditorDirty(false);
        owner.showPurchaseSuccess("Encomenda " + saved.orderNumber() + " " + action + ".");
        Container parent = panel.getEditorHost().getParent();
        if (parent != null && parent.getLayout() instanceof CardLayout layout) layout.show(parent, "LIST");
        panel.refresh();
        owner.reorderPanel.refresh();
    }

    void receiveSelectedPO() {
        PurchaseOrderDTO sel = panel.selectedPO();
        if (sel == null) return;
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.WARNING,
                "Confirmar recepção",
                "Receber a encomenda " + sel.orderNumber() + "? O stock do armazém será actualizado e a dívida ao fornecedor fica registada.",
                "Receber encomenda")) return;
        UIHelper.runWithProgress(owner, "A receber encomenda...", () -> owner.purchaseApiClient.receiveOrder(sel.id()), ignored -> {
            owner.showPurchaseSuccess("Encomenda recebida · stock e dívida ao fornecedor actualizados.");
            panel.refresh();
            owner.loadPurchasesHistory();
        }, owner::showPurchaseError);
    }

    void receivePartial(PurchaseOrderDTO sel, ReceivePurchaseOrderRequest request) {
        UIHelper.runWithProgress(owner, "A registar recepção parcial...",
                () -> owner.purchaseApiClient.receivePartial(sel.id(), request), updated -> {
                    owner.showPurchaseSuccess("Recepção registada · " + UIHelper.humanStatus(updated.status()) + ".");
                    panel.refresh();
                    owner.loadPurchasesHistory();
                }, owner::showPurchaseError);
    }

    void cancelSelectedPO() {
        PurchaseOrderDTO sel = panel.selectedPO();
        if (sel == null) return;
        String reason = UIHelper.promptRequiredText("Cancelar Encomenda", "fas-ban",
                "Encomenda " + sel.orderNumber(), "Motivo do cancelamento:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A cancelar encomenda...", () -> {
            owner.purchaseApiClient.cancelOrder(sel.id(), reason);
            return null;
        }, ignored -> panel.refresh(), owner::showPurchaseError);
    }

    void printSelectedPOReceiptLabels() {
        PurchaseOrderDTO sel = panel.selectedPO();
        if (sel == null) return;
        List<ProductDTO> products = PurchaseOrdersPanel.extractReceivedProducts(sel);
        if (products.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Sem artigos recebidos",
                    "Esta encomenda ainda não possui quantidades recebidas para emissão de etiquetas.");
            return;
        }
        ShelfLabelsDialog.show(SwingUtilities.getWindowAncestor(owner), products);
    }

    private void selectSupplier(Long id) {
        for (int i = 0; i < owner.supplierComboList.size(); i++) {
            if (id != null && id.equals(owner.supplierComboList.get(i).id())) {
                owner.poSupplierCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectWarehouse(Long id) {
        for (int i = 0; i < owner.warehousesList.size(); i++) {
            if (id != null && id.equals(owner.warehousesList.get(i).id())) {
                owner.poWarehouseCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    void populatePeek(QuickPeekPanel peek, int modelRow) {
        if (owner.poList == null || modelRow < 0 || modelRow >= owner.poList.size()) return;
        PurchaseOrderDTO order = owner.poList.get(modelRow);
        String status = order.status() != null ? order.status() : "PENDING";
        Color statusColor = "RECEIVED".equalsIgnoreCase(status) ? UIHelper.APPROVED_GREEN
                : ("CANCELLED".equalsIgnoreCase(status) ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);

        peek.setHeaderIcon("fas-clipboard-check", UIHelper.ACCENT_BLUE);
        peek.setTitle("Encomenda " + (order.orderNumber() != null ? order.orderNumber() : ""));
        peek.setSubtitle(order.supplierName() != null ? order.supplierName() : "—");
        peek.setStatus(UIHelper.humanStatus(status), statusColor);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<QuickPeekPanel.PeekItem> items = new java.util.ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Fornecedor", order.supplierName() != null ? order.supplierName() : "-", false));
        if (order.businessUnitName() != null && !order.businessUnitName().isBlank()) {
            items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", order.businessUnitName(), false));
        }
        items.add(new QuickPeekPanel.PeekItem("Data Emissão", order.orderDate() != null ? order.orderDate().format(dtf) : "-", false));
        items.add(new QuickPeekPanel.PeekItem("Entrega Prevista", order.expectedDate() != null ? order.expectedDate().toString() : "-", false));
        int itemCount = order.lines() != null ? order.lines().size() : 0;
        items.add(new QuickPeekPanel.PeekItem("Itens Encomendados", String.valueOf(itemCount), false));
        BigDecimal total = order.totalAmount() != null ? order.totalAmount() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Valor Total", String.format("%,.2f MT", total), true));
        peek.setItems(items);

        if ("ORDERED".equalsIgnoreCase(status) || "PARTIALLY_RECEIVED".equalsIgnoreCase(status)) {
            peek.getOpenFullButton().setText("Receber Encomenda");
            peek.setOnOpenFullAction(ignored -> receiveSelectedPO());
        } else {
            peek.setOnOpenFullAction(null);
        }
    }

    static List<ProductDTO> extractReceivedProducts(PurchaseOrderDTO order) {
        List<ProductDTO> result = new java.util.ArrayList<>();
        if (order == null || order.lines() == null) return result;
        for (PurchaseOrderLineDTO line : order.lines()) {
            BigDecimal qty = line.receivedQuantity();
            if (qty != null && qty.signum() > 0) {
                String sku = line.productSku() != null && !line.productSku().isBlank()
                        ? line.productSku() : "ART-" + line.productId();
                String barcode = (line.productSku() != null && !line.productSku().isBlank())
                        ? line.productSku()
                        : (line.serialNumber() != null ? line.serialNumber() : sku);
                BigDecimal price = line.unitPrice() != null ? line.unitPrice() : BigDecimal.ZERO;
                result.add(new ProductDTO(
                        line.productId(), sku, sku, barcode, line.productName(), price, price,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, "UNIT", true,
                        null, "Geral", null, line.taxRate() != null ? line.taxRate() : BigDecimal.ZERO,
                        "IVA", "Entrada por Encomenda", null, BigDecimal.ZERO, BigDecimal.ZERO
                ));
            }
        }
        return result;
    }
}

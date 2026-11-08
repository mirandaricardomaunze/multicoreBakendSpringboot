package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ProductLookupDialog;
import mz.multicore.erp.gui.components.TableExportAction;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import java.awt.Component;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Operações e ciclo de vida de encomendas e pedidos no módulo comercial:
 * editor, rascunhos, listagem paginada, aprovação, separação, conversão e exportação.
 */
final class CommercialOrderOperations {

    private static final DateTimeFormatter DTF_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CommercialOrderOperations() {}

    static void openOrderEditor(ComercialPanel owner) {
        if (owner.warehousesList.isEmpty()) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Armazém necessário", "Registe um armazém para a empresa actual.");
            return;
        }
        resetOrderDraft(owner);
        owner.editingOrder = null;
        owner.lastCreatedOrder = null;
        OrderEditorExtras.reset(owner);
        owner.orderGridEditable = true;
        if (owner.orderBarcodeField != null) owner.orderBarcodeField.setEnabled(true);
        owner.orderKindCombo.setEnabled(true);
        owner.orderEditor.setEditorTitle("Nova Encomenda");
        owner.orderEditor.setSaveText("Guardar encomenda");
        owner.orderEditor.setSaveEnabled(true);
        owner.clearOrderEditorDirty();
        if (owner.encomendasCards != null && owner.encomendasHost != null) {
            owner.encomendasCards.show(owner.encomendasHost, "editor");
        }
    }

    static void openSelectedOrderEditor(ComercialPanel owner) {
        CommercialOrderEditorActions.openSelected(owner);
    }

    static void backToOrdersList(ComercialPanel owner) {
        if (owner.encomendasCards != null && owner.encomendasHost != null) {
            owner.encomendasCards.show(owner.encomendasHost, "list");
        }
        owner.editingOrder = null;
        owner.orderGridEditable = false;
        if (owner.orderBarcodeField != null) owner.orderBarcodeField.setEnabled(false);
        owner.clearOrderEditorDirty();
    }

    static void saveOrderFromEditor(ComercialPanel owner) {
        CommercialOrderSubmission.save(owner, owner.comercialApiClient);
    }

    static void openOrderProductLookup(ComercialPanel owner) {
        if (!owner.orderGridEditable) return;
        if (owner.orderLinesTable.isEditing()) owner.orderLinesTable.getCellEditor().stopCellEditing();
        List<ProductDTO> selected;
        Component parent = owner.orderFormContent != null ? owner.orderFormContent : owner.ordersTable;
        if (owner.comercialApiClient != null) {
            selected = ProductLookupDialog.showMultiRemote(
                    parent,
                    (query, categoryId, page, size) -> owner.comercialApiClient.getProductsPage(query, categoryId, page, size)
            );
        } else {
            List<ProductDTO> catalog = owner.productsList;
            if (catalog == null || catalog.isEmpty()) {
                owner.showCommercialNotice(FeedbackType.WARNING, "Catálogo Vazio", "Não existem produtos registados no sistema.");
                return;
            }
            selected = ProductLookupDialog.showMulti(parent, catalog);
        }
        if (selected != null && !selected.isEmpty()) {
            CommercialOrderEditorActions.applyProductsFromLookup(owner, selected);
        }
    }

    static void addDraftOrderLine(ComercialPanel owner) {
        CommercialOrderEditorActions.addBlankLine(owner);
    }

    static void removeSelectedDraftOrderLine(ComercialPanel owner) {
        CommercialOrderEditorActions.removeSelectedLine(owner);
    }

    static void refreshOrderDraftTable(ComercialPanel owner) {
        CommercialOrderEditorActions.refreshTable(owner);
    }

    static mz.multicore.erp.modules.comercial.model.OrderKind selectedOrderKind(ComercialPanel owner) {
        return CommercialOrderSubmission.selectedKind(owner);
    }

    static void resetOrderDraft(ComercialPanel owner) {
        owner.draftOrderLines.clear();
        owner.draftOrderUnitPrices.clear();
        owner.draftOrderSubtotal = BigDecimal.ZERO;
        owner.draftOrderTax = BigDecimal.ZERO;
        owner.draftOrderTotal = BigDecimal.ZERO;
        if (owner.orderLinesTableModel != null) owner.orderLinesTableModel.setRowCount(0);
        if (owner.orderTotalLabel != null) owner.orderTotalLabel.setText("Total Rascunho: 0,00 MT (incl. IVA)");
        if (owner.orderLoadLabel != null) owner.orderLoadLabel.setText("Carga: 0.000 kg");
        if (owner.orderClientWalkInField != null) owner.orderClientWalkInField.setText("");
        if (owner.orderNotesField != null) owner.orderNotesField.setText("");
        if (owner.orderClientCombo != null && owner.orderClientCombo.getItemCount() > 0) owner.orderClientCombo.setSelectedIndex(0);
        if (owner.orderPaymentTermsCombo != null) {
            owner.orderPaymentTermsCombo.setSelectedItem("Pronto Pagamento");
            owner.orderPaymentTermsCombo.setEnabled(false);
        }
        if (owner.orderClientCreditWarningLabel != null) {
            owner.orderClientCreditWarningLabel.setVisible(false);
        }
    }

    static void billSelectedOrder(ComercialPanel owner) {
        CommercialOrderActions.billSelectedOrder(owner);
    }

    static void promptApproveOrRejectOrder(ComercialPanel owner, Long orderId, String orderNum) {
        CommercialOrderActions.promptApproveOrRejectOrder(owner, orderId, orderNum);
    }

    static void convertSelectedOrderToGuide(ComercialPanel owner) {
        CommercialOrderActions.convertSelectedOrderToGuide(owner);
    }

    static void generateProductionOrderFromSelectedOrder(ComercialPanel owner) {
        CommercialOrderActions.generateProductionOrderFromSelectedOrder(owner);
    }

    static void convertSelectedOrderToTransfer(ComercialPanel owner) {
        OrderToTransferAction.convertSelected(owner, owner.comercialApiClient);
    }

    static void loadOrdersTable(ComercialPanel owner) {
        if (owner.ordersPager != null) {
            owner.ordersPager.reload();
        } else {
            loadOrdersPage(owner, 0, 50);
        }
    }

    static void loadOrdersPage(ComercialPanel owner, int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        mz.multicore.erp.gui.components.UIHelper.loadAsync(owner, () -> owner.comercialApiClient.getOrdersPage(companyId, page, size),
                response -> {
                    if (owner.ordersPager != null) owner.ordersPager.apply(response);
                    applyOrders(owner, response != null ? response.items() : List.of());
                },
                error -> owner.showCommercialLoadError("encomendas", error));
    }

    static void applyOrders(ComercialPanel owner, List<OrderDTO> orders) {
        owner.setCachedOrders(orders != null ? new ArrayList<>(orders) : new ArrayList<>());
        owner.ordersTableModel.setRowCount(0);
        for (OrderDTO order : owner.getCachedOrders()) {
            String clientLabel = order.clientName();
            if (order.walkInName() != null && !order.walkInName().isBlank()) {
                clientLabel += " — " + order.walkInName();
            }
            String impressoes;
            if (order.printCount() <= 0) {
                impressoes = "—";
            } else if (order.printedAt() != null) {
                impressoes = order.printCount() + " × " + order.printedAt().format(DTF_SHORT);
            } else {
                impressoes = String.valueOf(order.printCount());
            }
            owner.ordersTableModel.addRow(new Object[]{
                    order.id(),
                    order.orderNumber(),
                    clientLabel,
                    order.status(),
                    order.totalAmount(),
                    impressoes,
                    order.kind(),
                    order.quotationNumber() == null ? "Directa" : "Cotação " + order.quotationNumber(),
                    formatExpectedDelivery(order),
                    order.businessUnitName() != null && !order.businessUnitName().isBlank()
                            ? order.businessUnitName() : "—"
            });
        }
        OrderPrintColumn.refresh(owner, owner.getCachedOrders());
    }

    static OrderDTO findCachedOrder(ComercialPanel owner, Long orderId) {
        if (orderId == null) return null;
        for (OrderDTO o : owner.getCachedOrders()) {
            if (orderId.equals(o.id())) return o;
        }
        return null;
    }

    static String formatExpectedDelivery(OrderDTO order) {
        if (order.expectedDeliveryDate() == null) return "Sem data";
        String date = order.expectedDeliveryDate().format(DTF_SHORT);
        return order.deliveryOverdue() ? date + " (em atraso)" : date;
    }

    static void openBillFromOrderDialog(ComercialPanel owner) {
        owner.getBillOrderDialog().open();
    }

    static void openCancelOrderDialog(ComercialPanel owner) {
        owner.getCancelOrderDialog().open();
    }

    static void printSelectedOrder(ComercialPanel owner) {
        CustomerOrderFulfillmentActions.printSelected(owner, owner.comercialApiClient);
    }

    static void completeSelectedOrderSeparation(ComercialPanel owner) {
        CustomerOrderFulfillmentActions.completeSeparation(owner, owner.comercialApiClient);
    }

    static void showSelectedOrderEvents(ComercialPanel owner) {
        CustomerOrderFulfillmentActions.showEvents(owner, owner.comercialApiClient);
    }

    static void openSelectedOrderDetails(ComercialPanel owner) {
        int row = TableFilter.selectedModelRow(owner.ordersTable);
        if (row < 0) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda", "Escolha uma encomenda na tabela para continuar.");
            return;
        }
        if (owner.encomendasCards != null && owner.encomendasHost != null) {
            owner.encomendasCards.show(owner.encomendasHost, "list");
        }
        owner.orderDetailsDialog.open((Long) owner.ordersTableModel.getValueAt(row, 0));
    }

    static void exportOrdersTable(ComercialPanel owner) {
        TableExportAction.export(owner, owner.printApiClient, owner.ordersTable, "Encomendas", "encomendas");
    }
}

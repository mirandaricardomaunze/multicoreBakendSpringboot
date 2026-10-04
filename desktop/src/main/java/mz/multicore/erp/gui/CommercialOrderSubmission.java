package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.UpdateOrderRequest;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Emissão de uma encomenda a partir do editor: valida o rascunho e envia-o pela porta da sua via.
 *
 * <p>As duas vias saem daqui por caminhos diferentes — o pedido de separação reserva stock e segue
 * para o armazém; a encomenda A4 vai para o motor de aprovações. Ver
 * {@code docs/ENCOMENDA_DUAS_VIAS_SPEC.md}.
 */
final class CommercialOrderSubmission {
    private CommercialOrderSubmission() {}

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    static void save(ComercialPanel owner, ComercialApiClient api) {
        CommercialOrderEditorActions.stopCellEditing(owner);
        try {
            if (owner.editingOrder != null) {
                UpdateOrderRequest request = buildUpdateRequest(owner);
                UIHelper.runWithProgress(owner, "A guardar alterações da encomenda…",
                        () -> api.updateOrder(owner.editingOrder.id(), request),
                        updated -> announceUpdate(owner, updated),
                        error -> owner.showCommercialError("actualizar encomenda", error));
                return;
            }
            CreateOrderRequest request = buildRequest(owner);
            if (request.effectiveKind().requiresApproval()) {
                UIHelper.runWithProgress(owner, "A registar encomenda e submeter a aprovação…",
                        () -> api.createOrder(request),
                        created -> announce(owner, created,
                                "Submetida a aprovação (" + created.totalAmount() + " MT)."),
                        error -> owner.showCommercialError("criar encomenda", error));
                return;
            }
            if (request.effectiveKind() == OrderKind.INTERNAL_REPLENISHMENT) {
                UIHelper.runWithProgress(owner, "A registar reposição interna…",
                        () -> api.createOrder(request),
                        created -> announce(owner, created,
                                "Pronta para conversão em transferência entre armazéns."),
                        error -> owner.showCommercialError("criar reposição interna", error));
                return;
            }
            String idempotencyKey = UUID.randomUUID().toString();
            UIHelper.runWithProgress(owner, "A enviar pedido e reservar stock…",
                    () -> api.submitFulfillmentOrder(request, idempotencyKey,
                            CustomerOrderFulfillmentActions.terminalName()),
                    created -> announce(owner, created,
                            "Enviado para separação e stock reservado (" + created.totalAmount() + " MT)."),
                    error -> owner.showCommercialError("criar encomenda", error));
        } catch (Exception ex) {
            owner.showCommercialError("criar encomenda", ex);
        }
    }

    private static void announce(ComercialPanel owner, OrderDTO created, String estado) {
        owner.lastCreatedOrder = created;
        owner.clearOrderEditorDirty();
        owner.showCommercialSuccess("Encomenda " + created.orderNumber() + " criada. " + estado);
        owner.loadOrdersTable();
        owner.backToOrdersList();
    }

    private static void announceUpdate(ComercialPanel owner, OrderDTO updated) {
        owner.lastCreatedOrder = updated;
        owner.editingOrder = updated;
        owner.clearOrderEditorDirty();
        owner.showCommercialSuccess("Encomenda " + updated.orderNumber() + " actualizada com sucesso.");
        owner.loadOrdersTable();
        owner.backToOrdersList();
    }

    private static UpdateOrderRequest buildUpdateRequest(ComercialPanel owner) {
        if (owner.editingOrder == null) {
            throw new RuntimeException("Seleccione a encomenda que pretende actualizar.");
        }
        if (owner.draftOrderLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à encomenda.");
        }
        validateLines(owner);
        int warehouseIndex = owner.orderWarehouseCombo.getSelectedIndex();
        if (warehouseIndex < 0 || warehouseIndex >= owner.warehousesList.size()) {
            throw new RuntimeException("Seleccione o armazém.");
        }
        Long clientId = null;
        String walkInName = null;
        int clientIndex = owner.orderClientCombo.getSelectedIndex();
        if (clientIndex > 0 && clientIndex - 1 < owner.clientsList.size()) {
            clientId = owner.clientsList.get(clientIndex - 1).id();
        } else {
            String typed = owner.orderClientWalkInField.getText().trim();
            if (!typed.isEmpty()) walkInName = typed;
        }
        WarehouseDTO warehouse = owner.warehousesList.get(warehouseIndex);
        Long destinationId = owner.editingOrder.kind().requiresDestinationWarehouse()
                ? destinationWarehouseId(owner, warehouse) : null;
        return new UpdateOrderRequest(owner.editingOrder.version(), clientId, walkInName,
                warehouse.id(), destinationId, new ArrayList<>(owner.draftOrderLines));
    }

    /** Validação do rascunho. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private static CreateOrderRequest buildRequest(ComercialPanel owner) {
        if (owner.warehousesList.isEmpty()) {
            throw new RuntimeException("Nenhum armazém disponível para a empresa atual.");
        }
        if (owner.draftOrderLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à encomenda.");
        }
        validateLines(owner);
        int clientIdx = owner.orderClientCombo.getSelectedIndex();
        int whIdx = owner.orderWarehouseCombo.getSelectedIndex();
        if (whIdx < 0) {
            throw new RuntimeException("Selecione o armazém.");
        }

        // O índice 0 do combo é "Consumidor Final"; índices >0 mapeiam para clientsList[idx-1].
        Long clientId = null;
        String walkInName = null;
        if (clientIdx > 0 && (clientIdx - 1) < owner.clientsList.size()) {
            clientId = owner.clientsList.get(clientIdx - 1).id();
        } else {
            String typed = owner.orderClientWalkInField == null ? "" : owner.orderClientWalkInField.getText().trim();
            if (!typed.isEmpty()) walkInName = typed;
        }

        WarehouseDTO warehouse = owner.warehousesList.get(whIdx);
        OrderKind kind = selectedKind(owner);
        if (kind.requiresDestinationWarehouse()) {
            return CreateOrderRequest.replenishment(CurrentUserContext.getCurrentCompanyId(),
                    warehouse.id(), destinationWarehouseId(owner, warehouse),
                    new ArrayList<>(owner.draftOrderLines));
        }
        return new CreateOrderRequest(clientId, walkInName, CurrentUserContext.getCurrentCompanyId(),
                warehouse.id(), new ArrayList<>(owner.draftOrderLines), kind);
    }

    /** A loja que recebe. Validado aqui só para o operador não ir ao servidor descobrir o óbvio. */
    private static Long destinationWarehouseId(ComercialPanel owner, WarehouseDTO origin) {
        int index = owner.orderDestinationCombo == null ? -1 : owner.orderDestinationCombo.getSelectedIndex();
        if (index < 0 || index >= owner.warehousesList.size()) {
            throw new RuntimeException("Seleccione o armazém de destino: uma reposição interna tem de "
                    + "dizer para que loja vai a mercadoria.");
        }
        WarehouseDTO destination = owner.warehousesList.get(index);
        if (destination.id().equals(origin.id())) {
            throw new RuntimeException("O armazém de destino tem de ser diferente do de origem.");
        }
        return destination.id();
    }

    private static void syncLinesBeforeSave(ComercialPanel owner) {
        for (int row = 0; row < owner.draftOrderLines.size(); row++) {
            var line = owner.draftOrderLines.get(row);
            if (line.productId() == null || line.productId() == 0L) {
                CommercialOrderEditorActions.syncLineFromGrid(owner, row, 0);
            }
        }
    }

    private static void validateLines(ComercialPanel owner) {
        syncLinesBeforeSave(owner);
        for (int row = 0; row < owner.draftOrderLines.size(); row++) {
            var line = owner.draftOrderLines.get(row);
            boolean productExists = owner.productsList.stream()
                    .anyMatch(product -> product.id().equals(line.productId()));
            if (!productExists) throw new RuntimeException("Seleccione o produto da linha " + (row + 1) + ".");
            if (line.quantity() == null || line.quantity().signum() <= 0) {
                throw new RuntimeException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
            }
            var discount = line.discountPercentage() == null ? java.math.BigDecimal.ZERO
                    : line.discountPercentage();
            if (discount.signum() < 0 || discount.compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("O desconto da linha " + (row + 1) + " deve ficar entre 0 e 100.");
            }
        }
    }

    /** Via escolhida no editor. Sem escolha feita, o pedido de separação — o uso diário do balcão. */
    static OrderKind selectedKind(ComercialPanel owner) {
        Object selected = owner.orderKindCombo == null ? null : owner.orderKindCombo.getSelectedItem();
        return selected instanceof OrderKind kind ? kind : OrderKind.PICKING_REQUEST;
    }
}

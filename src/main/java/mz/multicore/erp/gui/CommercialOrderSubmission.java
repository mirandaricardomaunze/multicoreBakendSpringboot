package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
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
        try {
            CreateOrderRequest request = buildRequest(owner);
            if (request.effectiveKind().requiresApproval()) {
                UIHelper.runWithProgress(owner, "A registar encomenda e submeter a aprovação…",
                        () -> api.createOrder(request),
                        created -> announce(owner, created,
                                "Submetida a aprovação (" + created.totalAmount() + " MT)."),
                        error -> owner.showCommercialError("criar encomenda", error));
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
            JOptionPane.showMessageDialog(owner,
                    ex.getMessage() == null ? "Falha ao criar encomenda." : ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void announce(ComercialPanel owner, OrderDTO created, String estado) {
        owner.lastCreatedOrder = created;
        JOptionPane.showMessageDialog(owner, "Encomenda " + created.orderNumber() + " criada!\n" + estado,
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        owner.loadOrdersTable();
        owner.backToOrdersList();
    }

    /** Validação do rascunho. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private static CreateOrderRequest buildRequest(ComercialPanel owner) {
        if (owner.warehousesList.isEmpty()) {
            throw new RuntimeException("Nenhum armazém disponível para a empresa atual.");
        }
        if (owner.draftOrderLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à encomenda.");
        }
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

    /** Via escolhida no editor. Sem escolha feita, o pedido de separação — o uso diário do balcão. */
    static OrderKind selectedKind(ComercialPanel owner) {
        Object selected = owner.orderKindCombo == null ? null : owner.orderKindCombo.getSelectedItem();
        return selected instanceof OrderKind kind ? kind : OrderKind.PICKING_REQUEST;
    }
}

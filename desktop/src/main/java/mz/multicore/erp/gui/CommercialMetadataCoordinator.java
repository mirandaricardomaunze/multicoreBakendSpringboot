package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de carregamento e aplicação de metadados do módulo comercial:
 * Clientes, produtos do catálogo, armazéns de expedição e unidades de negócio.
 */
final class CommercialMetadataCoordinator {

    private CommercialMetadataCoordinator() {}

    record CommercialMetadata(List<ClientDTO> clients, List<ProductDTO> products) {}

    static void loadClientsAndProducts(ComercialPanel owner) {
        UIHelper.loadAsync(owner, () -> new CommercialMetadata(
                        owner.comercialApiClient.getAllClients(),
                        owner.comercialApiClient.getAllProducts()),
                metadata -> applyClientsAndProducts(owner, metadata),
                error -> owner.showCommercialLoadError("clientes e produtos", error));
    }

    static void applyClientsAndProducts(ComercialPanel owner, CommercialMetadata metadata) {
        owner.clientCombo.removeAllItems();
        owner.orderClientCombo.removeAllItems();
        owner.clientsList = metadata.clients();
        owner.productsList.clear();
        owner.productsList.addAll(metadata.products());

        owner.clientCombo.addItem("Consumidor Final (sem registo)");
        owner.orderClientCombo.addItem("Consumidor Final (sem registo)");

        for (ClientDTO c : owner.clientsList) {
            owner.clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
            owner.orderClientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }

        owner.productCombo.setProducts(owner.productsList);
        owner.orderProductCombo.setProducts(owner.productsList);
    }

    static void loadWarehouses(ComercialPanel owner) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.getInventoryApiClient().getWarehousesByCompany(companyId),
                loaded -> applyWarehouses(owner, loaded),
                error -> owner.showCommercialLoadError("armazéns", error));
    }

    static void applyWarehouses(ComercialPanel owner, List<WarehouseDTO> loaded) {
        owner.warehouseCombo.removeAllItems();
        owner.orderWarehouseCombo.removeAllItems();
        if (owner.orderDestinationCombo != null) owner.orderDestinationCombo.removeAllItems();
        owner.warehousesList = loaded;

        for (WarehouseDTO w : owner.warehousesList) {
            owner.warehouseCombo.addItem(w.name());
            owner.orderWarehouseCombo.addItem(w.name());
            if (owner.orderDestinationCombo != null) owner.orderDestinationCombo.addItem(w.name());
        }
    }

    static void loadBusinessUnits(ComercialPanel owner) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(owner, () -> owner.comercialApiClient.getActiveBusinessUnits(companyId),
                loaded -> applyBusinessUnits(owner, loaded),
                error -> { /* falha suave em linhas opcionais */ });
    }

    static void applyBusinessUnits(ComercialPanel owner, List<BusinessUnitDTO> loaded) {
        owner.businessUnitsList = loaded != null ? loaded : new ArrayList<>();
        if (owner.invoiceBusinessUnitCombo != null) {
            owner.invoiceBusinessUnitCombo.removeAllItems();
            owner.invoiceBusinessUnitCombo.addItem(new ComercialPanel.BusinessUnitOption(null, "— Misto (Sem restrição) —"));
            for (BusinessUnitDTO bu : owner.businessUnitsList) {
                owner.invoiceBusinessUnitCombo.addItem(new ComercialPanel.BusinessUnitOption(bu.id(), bu.code() + " - " + bu.name()));
            }
        }
        if (owner.invBusinessUnitCombo != null) {
            Object current = owner.invBusinessUnitCombo.getSelectedItem();
            owner.invBusinessUnitCombo.removeAllItems();
            owner.invBusinessUnitCombo.addItem("Todos os negócios");
            owner.invBusinessUnitCombo.addItem("Misto");
            for (BusinessUnitDTO bu : owner.businessUnitsList) {
                owner.invBusinessUnitCombo.addItem(bu.name());
            }
            if (current != null) owner.invBusinessUnitCombo.setSelectedItem(current);
        }
        if (owner.ecBusinessUnitCombo != null) {
            Object current = owner.ecBusinessUnitCombo.getSelectedItem();
            owner.ecBusinessUnitCombo.removeAllItems();
            owner.ecBusinessUnitCombo.addItem("Todos os negócios");
            owner.ecBusinessUnitCombo.addItem("—");
            for (BusinessUnitDTO bu : owner.businessUnitsList) {
                owner.ecBusinessUnitCombo.addItem(bu.name());
            }
            if (current != null) owner.ecBusinessUnitCombo.setSelectedItem(current);
        }
    }
}

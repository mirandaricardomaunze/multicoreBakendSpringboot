package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.BarcodeScannerCollectorDialog;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ShelfLabelsDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de armazéns, saldos de stock, coletores e impressão de inventário valorizado.
 */
public class StockWarehouseCoordinator {

    private final StockPanel owner;

    public StockWarehouseCoordinator(StockPanel owner) {
        this.owner = owner;
    }

    public void loadWarehouses() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.getWarehousesByCompany(companyId),
                this::applyWarehouses, error -> owner.showStockLoadError("armazéns", error));
    }

    public void applyWarehouses(List<WarehouseDTO> loaded) {
        owner.warehousesList = loaded;
        if (owner.warehouseFilterCombo == null) return;
        Object selectedItem = owner.warehouseFilterCombo.getSelectedItem();
        owner.warehouseFilterCombo.removeAllItems();
        owner.warehouseFilterCombo.addItem("Todos os Armazéns");
        for (WarehouseDTO w : owner.warehousesList) {
            owner.warehouseFilterCombo.addItem(w.name());
        }
        if (selectedItem != null) {
            owner.warehouseFilterCombo.setSelectedItem(selectedItem);
        }
    }

    public void loadStocks() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.getStocksByCompany(companyId),
                this::applyStocks, error -> owner.showStockLoadError("saldos de stock", error));
    }

    public void applyStocks(List<StockDTO> loaded) {
        owner.stocksList = loaded;
        StockLevelsFilter.syncCategoryCombo(owner);
        StockLevelsFilter.syncBusinessUnitCombo(owner);
        StockLevelsFilter.filterStocks(owner);
        if (owner.warehousesPanel != null) {
            owner.warehousesPanel.updateKpis();
        }
    }

    public Long selectedStockProductId() {
        if (owner.stockTable == null) return null;
        int viewRow = owner.stockTable.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = owner.stockTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= owner.stockRowProductIds.size()) return null;
        return owner.stockRowProductIds.get(modelRow);
    }

    public void openLabelDialog() {
        Long selectedId = selectedStockProductId();
        List<ProductDTO> products = new ArrayList<>();
        if (selectedId != null) {
            owner.catalogProducts.stream().filter(p -> p.id().equals(selectedId)).findFirst().ifPresent(products::add);
        }
        if (products.isEmpty()) {
            products.addAll(owner.catalogProducts);
        }
        if (products.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário", "Registe um produto antes de criar etiquetas.");
            return;
        }
        ShelfLabelsDialog.show(SwingUtilities.getWindowAncestor(owner), products);
    }

    public void openInventoryBarcodeCollector() {
        var collectorDialog = new BarcodeScannerCollectorDialog(
                SwingUtilities.getWindowAncestor(owner),
                BarcodeScannerCollectorDialog.CollectorMode.INVENTORY,
                owner.comercialApiClient,
                null,
                owner.inventoryApiClient,
                this::loadStocks
        );
        collectorDialog.showDialog();
    }

    public void printInventoryReport() {
        StockReportsCoordinator.printInventoryReport(owner);
    }

    public void printValuedInventoryBook() {
        StockReportsCoordinator.printValuedInventoryBook(owner);
    }
}

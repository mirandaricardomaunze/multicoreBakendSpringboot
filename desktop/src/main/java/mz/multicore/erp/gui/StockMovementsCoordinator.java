package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.StockMovementDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Coordenador de paginação, filtros e dados da aba de Movimentos & Rastreabilidade de Stock.
 */
public class StockMovementsCoordinator {

    private final StockPanel owner;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public StockMovementsCoordinator(StockPanel owner) {
        this.owner = owner;
    }

    public void loadMovements() {
        if (owner.movementsPager != null) {
            owner.movementsPager.reload();
        } else {
            loadMovementsPage(0, 50);
        }
    }

    public void loadMovementsPage(int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.getMovementsPageByCompany(companyId, page, size),
                this::applyMovementsPage, error -> owner.showStockLoadError("movimentos", error));
    }

    public void applyMovementsPage(PageResponse<StockMovementDTO> response) {
        if (owner.movementsPager != null) owner.movementsPager.apply(response);
        applyMovements(response != null ? response.items() : List.of());
    }

    public void applyMovements(List<StockMovementDTO> movements) {
        if (owner.movementsModel == null) return;
        owner.movementsModel.setRowCount(0);

        Object selectedProduct = owner.movementProductFilter == null ? null : owner.movementProductFilter.getSelectedItem();
        TreeSet<String> productNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (movements != null) {
            for (StockMovementDTO movement : movements) {
                if (movement.productName() != null && !movement.productName().isBlank()) {
                    productNames.add(movement.productName());
                }
            }
        }
        if (owner.catalogProducts != null) {
            for (ProductDTO prod : owner.catalogProducts) {
                if (prod.name() != null && !prod.name().isBlank()) {
                    productNames.add(prod.name());
                }
            }
        }
        if (owner.movementProductFilter != null) {
            owner.movementProductFilter.removeAllItems();
            owner.movementProductFilter.addItem("Todos os artigos");
            productNames.forEach(owner.movementProductFilter::addItem);
            if (selectedProduct != null) owner.movementProductFilter.setSelectedItem(selectedProduct);
        }
        if (owner.movementBusinessUnitFilter != null) {
            Object prevBu = owner.movementBusinessUnitFilter.getSelectedItem();
            Set<String> bus = new TreeSet<>();
            if (movements != null) {
                for (StockMovementDTO movement : movements) {
                    if (movement.businessUnitName() != null && !movement.businessUnitName().isBlank()) {
                        bus.add(movement.businessUnitName());
                    }
                }
            }
            owner.movementBusinessUnitFilter.removeAllItems();
            owner.movementBusinessUnitFilter.addItem("Todos os negócios");
            for (String b : bus) owner.movementBusinessUnitFilter.addItem(b);
            if (prevBu != null) owner.movementBusinessUnitFilter.setSelectedItem(prevBu);
        }

        for (StockMovementDTO m : movements == null ? List.<StockMovementDTO>of() : movements) {
            BigDecimal qty = m.quantity();
            String formattedQty = (qty.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + String.format("%,.2f", qty);
            String buName = m.businessUnitName() != null && !m.businessUnitName().isBlank() ? m.businessUnitName() : "—";
            owner.movementsModel.addRow(new Object[]{
                    m.movementDate().format(dtf),
                    m.productName(),
                    m.warehouseName(),
                    formattedQty,
                    UIHelper.humanStatus(m.movementType()),
                    m.batchNumber() != null ? m.batchNumber() : "-",
                    m.serialNumber() != null ? m.serialNumber() : "-",
                    m.description(),
                    buName
            });
        }
    }

    public void printFilteredMovements(JComboBox<String> productFilter, DateField fromField, DateField toField) {
        StockReportsCoordinator.printFilteredMovements(owner, productFilter, fromField, toField);
    }

    public void exportMovementsTable(String title, String baseName) {
        TableExportAction.export(owner, owner.printApiClient, owner.movementsTable, title, baseName);
    }

    public void createAdjustmentDialog() {
        StockReportsCoordinator.createAdjustmentDialog(owner);
    }
}

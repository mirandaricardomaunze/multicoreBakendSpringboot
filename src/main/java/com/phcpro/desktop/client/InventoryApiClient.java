package com.phcpro.desktop.client;

import com.phcpro.modules.inventory.dto.CreateStockAdjustmentRequest;
import com.phcpro.modules.inventory.dto.CreateWarehouseRequest;
import com.phcpro.modules.inventory.dto.ProductBatchDTO;
import com.phcpro.modules.inventory.dto.RegisterMovementRequest;
import com.phcpro.modules.inventory.dto.StockAlertDTO;
import com.phcpro.modules.inventory.dto.StockDTO;
import com.phcpro.modules.inventory.dto.StockMovementDTO;
import com.phcpro.modules.inventory.dto.UpdateWarehouseRequest;
import com.phcpro.modules.inventory.dto.WarehouseDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Cliente HTTP para o inventário ({@code /api/inventory}) + impressões de stock. */
@Component
@Profile("desktop")
public class InventoryApiClient {

    private final DesktopClientFactory clientFactory;

    public InventoryApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    // ─── Stocks ──────────────────────────────────────────────────────────────
    public List<StockDTO> getStocksByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/stocks?companyId=" + companyId, StockDTO.class);
    }

    public List<StockAlertDTO> findOutOfStockProducts(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/out-of-stock?companyId=" + companyId, StockAlertDTO.class);
    }

    // ─── Armazéns ────────────────────────────────────────────────────────────
    public List<WarehouseDTO> getWarehousesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/warehouses?companyId=" + companyId, WarehouseDTO.class);
    }

    public List<WarehouseDTO> getAllWarehousesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/warehouses/all?companyId=" + companyId, WarehouseDTO.class);
    }

    public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/warehouses", request, WarehouseDTO.class);
    }

    public WarehouseDTO updateWarehouse(Long id, UpdateWarehouseRequest request) {
        return clientFactory.authenticatedClient()
                .put("/api/inventory/warehouses/" + id, request, WarehouseDTO.class);
    }

    public WarehouseDTO setWarehouseActive(Long id, boolean active) {
        return clientFactory.authenticatedClient()
                .patch("/api/inventory/warehouses/" + id + "/active?active=" + active, null, WarehouseDTO.class);
    }

    // ─── Movimentos ──────────────────────────────────────────────────────────
    public List<StockMovementDTO> getMovementsByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/movements?companyId=" + companyId, StockMovementDTO.class);
    }

    public StockMovementDTO registerMovement(RegisterMovementRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/movements", request, StockMovementDTO.class);
    }

    public StockMovementDTO adjustStock(CreateStockAdjustmentRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/adjustments", request, StockMovementDTO.class);
    }

    // ─── Lotes / validades / FEFO ────────────────────────────────────────────
    public List<ProductBatchDTO> findBatchesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/batches?companyId=" + companyId, ProductBatchDTO.class);
    }

    public List<ProductBatchDTO> findExpiringBatches(Long companyId, int daysAhead) {
        LocalDate before = LocalDate.now().plusDays(daysAhead);
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/batches/expiring?companyId=" + companyId + "&before=" + before,
                        ProductBatchDTO.class);
    }

    public Optional<ProductBatchDTO> findNextFEFO(Long productId, Long warehouseId) {
        return Optional.ofNullable(clientFactory.authenticatedClient().get(
                "/api/inventory/batches/next-fefo?productId=" + productId + "&warehouseId=" + warehouseId,
                ProductBatchDTO.class));
    }

    // ─── Bloqueio de contagem ────────────────────────────────────────────────
    public boolean isStockCountLocked(Long companyId) {
        Boolean locked = clientFactory.authenticatedClient()
                .get("/api/inventory/stock-count-lock?companyId=" + companyId, Boolean.class);
        return Boolean.TRUE.equals(locked);
    }

    public void setStockCountLocked(Long companyId, boolean locked) {
        clientFactory.authenticatedClient()
                .post("/api/inventory/stock-count-lock?companyId=" + companyId + "&locked=" + locked, null);
    }

    // ─── Impressões (PDF) ────────────────────────────────────────────────────
    public byte[] renderInventoryReport(Long companyId, Long warehouseId) {
        String path = "/api/print/inventory?companyId=" + companyId;
        if (warehouseId != null) path += "&warehouseId=" + warehouseId;
        return clientFactory.authenticatedClient().getBytes(path);
    }

    public byte[] renderCountSheet(Long companyId, Long warehouseId) {
        String path = "/api/print/inventory-count-sheet?companyId=" + companyId;
        if (warehouseId != null) path += "&warehouseId=" + warehouseId;
        return clientFactory.authenticatedClient().getBytes(path);
    }

    public byte[] renderProductLabels(Long companyId, List<Long> productIds, int copies) {
        String ids = productIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        return clientFactory.authenticatedClient()
                .getBytes("/api/print/product-label?companyId=" + companyId + "&productIds=" + ids + "&copies=" + copies);
    }
}

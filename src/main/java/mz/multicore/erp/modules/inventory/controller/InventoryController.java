package mz.multicore.erp.modules.inventory.controller;

import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.inventory.service.ProductBatchService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductBatchService productBatchService;

    public InventoryController(InventoryService inventoryService,
                                ProductBatchService productBatchService) {
        this.inventoryService = inventoryService;
        this.productBatchService = productBatchService;
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<WarehouseDTO>> getWarehouses(@RequestParam Long companyId) {
        return ResponseEntity.ok(inventoryService.findWarehousesByCompany(companyId));
    }

    /** Armazéns que permitem vendas ao balcão (POS) — só estes aparecem no seletor de caixa. */
    @GetMapping("/warehouses/sales")
    public ResponseEntity<List<WarehouseDTO>> getSalesWarehouses(@RequestParam Long companyId) {
        return ResponseEntity.ok(inventoryService.getSalesWarehousesByCompany(companyId)
                .stream().map(inventoryService::toDTO).toList());
    }

    @PostMapping("/warehouses")
    public ResponseEntity<WarehouseDTO> createWarehouse(@RequestBody @Valid CreateWarehouseRequest request) {
        return ResponseEntity.ok(inventoryService.createWarehouse(request));
    }

    @GetMapping("/stocks")
    public ResponseEntity<List<StockDTO>> getStocks(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long warehouseId
    ) {
        if (warehouseId != null) {
            return ResponseEntity.ok(inventoryService.findStocksByWarehouse(warehouseId));
        }
        if (companyId != null) {
            return ResponseEntity.ok(inventoryService.findStocksByCompany(companyId));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/movements")
    public ResponseEntity<List<StockMovementDTO>> getMovements(@RequestParam Long companyId) {
        return ResponseEntity.ok(inventoryService.findMovementsByCompany(companyId));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockMovementDTO> adjustStock(@RequestBody @Valid CreateStockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(request));
    }

    @GetMapping("/batches")
    public ResponseEntity<List<ProductBatchDTO>> getBatches(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long warehouseId
    ) {
        if (warehouseId != null) {
            return ResponseEntity.ok(productBatchService.findByWarehouse(warehouseId));
        }
        if (companyId != null) {
            return ResponseEntity.ok(productBatchService.findByCompany(companyId));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/batches/expiring")
    public ResponseEntity<List<ProductBatchDTO>> getExpiringBatches(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before
    ) {
        return ResponseEntity.ok(productBatchService.findExpiringByCompany(companyId, before));
    }

    // ─── Gestão de armazéns (todos + editar + activar) ────────────────────────
    @GetMapping("/warehouses/all")
    public ResponseEntity<List<WarehouseDTO>> getAllWarehouses(@RequestParam Long companyId) {
        return ResponseEntity.ok(inventoryService.getAllWarehousesByCompany(companyId)
                .stream().map(inventoryService::toDTO).toList());
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<WarehouseDTO> updateWarehouse(@PathVariable Long id,
                                                        @RequestBody @Valid UpdateWarehouseRequest r) {
        return ResponseEntity.ok(inventoryService.toDTO(inventoryService.updateWarehouse(
                id, r.name(), r.warehouseNumber(), r.capacity(), r.location(), r.type(),
                r.allowsSales(), r.manager(), r.phone())));
    }

    @PatchMapping("/warehouses/{id}/active")
    public ResponseEntity<WarehouseDTO> setWarehouseActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(inventoryService.toDTO(inventoryService.setWarehouseActive(id, active)));
    }

    // ─── Movimentos de stock (entrada por id) ─────────────────────────────────
    @PostMapping("/movements")
    public ResponseEntity<StockMovementDTO> registerMovement(@RequestBody @Valid RegisterMovementRequest request) {
        return ResponseEntity.ok(inventoryService.registerMovement(request));
    }

    // ─── FEFO + rupturas ──────────────────────────────────────────────────────
    @GetMapping("/batches/next-fefo")
    public ResponseEntity<ProductBatchDTO> nextFefo(@RequestParam Long productId, @RequestParam Long warehouseId) {
        return inventoryService.findNextFEFO(productId, warehouseId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<List<StockAlertDTO>> outOfStock(@RequestParam Long companyId) {
        return ResponseEntity.ok(inventoryService.findOutOfStockProducts(companyId));
    }

    // ─── Bloqueio de contagem de stock (contagem cega) ────────────────────────
    @GetMapping("/stock-count-lock")
    public ResponseEntity<Boolean> isStockCountLocked(@RequestParam Long companyId) {
        return ResponseEntity.ok(inventoryService.isStockCountLocked(companyId));
    }

    @PostMapping("/stock-count-lock")
    public ResponseEntity<Void> setStockCountLocked(@RequestParam Long companyId, @RequestParam boolean locked) {
        inventoryService.setStockCountLocked(companyId, locked);
        return ResponseEntity.noContent().build();
    }
}

package com.phcpro.modules.inventory.controller;

import com.phcpro.modules.inventory.dto.*;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.inventory.service.ProductBatchService;
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
}

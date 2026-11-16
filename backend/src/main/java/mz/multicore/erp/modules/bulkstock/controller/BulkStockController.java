package mz.multicore.erp.modules.bulkstock.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.bulkstock.dto.*;
import mz.multicore.erp.modules.bulkstock.service.BulkStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bulk stock measured on the pile (SA-C17). Yard staff preview and record measurements; management sets
 * the bulk products and decides differences above tolerance (see {@code ApiRolePolicy}).
 */
@RestController
@RequestMapping("/api/bulk-stock")
public class BulkStockController {

    private final BulkStockService bulkStockService;

    public BulkStockController(BulkStockService bulkStockService) {
        this.bulkStockService = bulkStockService;
    }

    @GetMapping("/settings")
    public ResponseEntity<List<BulkStockSettingDTO>> settings(@RequestParam Long companyId) {
        return ResponseEntity.ok(bulkStockService.settings(companyId));
    }

    @PutMapping("/settings")
    public ResponseEntity<List<BulkStockSettingDTO>> saveSetting(@RequestBody @Valid SaveBulkStockSettingRequest request) {
        return ResponseEntity.ok(bulkStockService.saveSetting(request));
    }

    @GetMapping("/measurements")
    public ResponseEntity<List<BulkMeasurementDTO>> measurements(@RequestParam Long companyId) {
        return ResponseEntity.ok(bulkStockService.measurements(companyId));
    }

    @PostMapping("/measurements/preview")
    public ResponseEntity<BulkMeasurementDTO> preview(@RequestBody @Valid BulkMeasurementRequest request) {
        return ResponseEntity.ok(bulkStockService.preview(request));
    }

    @PostMapping("/measurements")
    public ResponseEntity<BulkMeasurementDTO> record(@RequestBody @Valid BulkMeasurementRequest request) {
        return ResponseEntity.ok(bulkStockService.record(request));
    }

    @PostMapping("/measurements/{id}/approve")
    public ResponseEntity<BulkMeasurementDTO> approve(@PathVariable Long id, @RequestBody @Valid ReviewBulkMeasurementRequest request) {
        return ResponseEntity.ok(bulkStockService.approve(id, request));
    }

    @PostMapping("/measurements/{id}/reject")
    public ResponseEntity<BulkMeasurementDTO> reject(@PathVariable Long id, @RequestBody @Valid ReviewBulkMeasurementRequest request) {
        return ResponseEntity.ok(bulkStockService.reject(id, request));
    }
}

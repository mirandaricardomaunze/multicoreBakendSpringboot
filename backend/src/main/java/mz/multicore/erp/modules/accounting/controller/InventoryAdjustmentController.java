package mz.multicore.erp.modules.accounting.controller;

import mz.multicore.erp.modules.accounting.dto.InventoryAdjustmentDTO;
import mz.multicore.erp.modules.accounting.service.InventoryAdjustmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Acerto das contas de existências ao stock físico (inventário inicial e desvios). Só HTTP. */
@RestController
@RequestMapping("/api/accounting")
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService inventoryAdjustmentService;

    public InventoryAdjustmentController(InventoryAdjustmentService inventoryAdjustmentService) {
        this.inventoryAdjustmentService = inventoryAdjustmentService;
    }

    @PostMapping("/inventory-adjustment")
    public ResponseEntity<InventoryAdjustmentDTO> adjust() {
        return ResponseEntity.ok(inventoryAdjustmentService.adjust());
    }
}

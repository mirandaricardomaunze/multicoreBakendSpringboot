package com.phcpro.modules.inventory.controller;

import com.phcpro.modules.inventory.dto.CreateInventoryCountRequest;
import com.phcpro.modules.inventory.dto.InventoryCountDTO;
import com.phcpro.modules.inventory.dto.SaveCountsRequest;
import com.phcpro.modules.inventory.service.InventoryCountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sessões de contagem de inventário (inventário físico): criar, listar, consultar, guardar contagens,
 * aplicar (gera os ajustes de stock) e cancelar. Só HTTP — a lógica e as regras vivem em
 * {@link InventoryCountService}.
 */
@RestController
@RequestMapping("/api/inventory/counts")
public class InventoryCountController {

    private final InventoryCountService inventoryCountService;

    public InventoryCountController(InventoryCountService inventoryCountService) {
        this.inventoryCountService = inventoryCountService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryCountDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(inventoryCountService.listSessions(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryCountDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCountService.getSession(id));
    }

    @PostMapping
    public ResponseEntity<InventoryCountDTO> create(@RequestBody @Valid CreateInventoryCountRequest request) {
        return ResponseEntity.ok(inventoryCountService.createSession(request.warehouseId(), request.note()));
    }

    @PutMapping("/{id}/counts")
    public ResponseEntity<InventoryCountDTO> saveCounts(@PathVariable Long id,
                                                        @RequestBody @Valid SaveCountsRequest request) {
        Map<Long, BigDecimal> counts = new HashMap<>();
        for (SaveCountsRequest.CountEntry entry : request.counts()) {
            counts.put(entry.productId(), entry.countedQuantity());
        }
        return ResponseEntity.ok(inventoryCountService.saveCounts(id, counts));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<InventoryCountDTO> apply(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCountService.applySession(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        inventoryCountService.cancelSession(id);
        return ResponseEntity.noContent().build();
    }
}

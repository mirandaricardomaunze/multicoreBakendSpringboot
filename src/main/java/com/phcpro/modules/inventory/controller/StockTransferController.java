package com.phcpro.modules.inventory.controller;

import com.phcpro.modules.inventory.dto.CreateStockTransferRequest;
import com.phcpro.modules.inventory.dto.StockTransferDTO;
import com.phcpro.modules.inventory.service.StockTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/transfers")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @GetMapping
    public ResponseEntity<List<StockTransferDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(stockTransferService.findByCompany(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockTransferDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stockTransferService.findById(id));
    }

    @PostMapping
    public ResponseEntity<StockTransferDTO> create(@RequestBody @Valid CreateStockTransferRequest request) {
        return ResponseEntity.ok(stockTransferService.create(request));
    }
}

package com.phcpro.modules.purchases.controller;

import com.phcpro.modules.purchases.dto.*;
import com.phcpro.modules.purchases.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDTO>> getSuppliers(@RequestParam Long companyId) {
        return ResponseEntity.ok(purchaseService.findSuppliersByCompany(companyId));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierDTO> createSupplier(@RequestBody @Valid CreateSupplierRequest request) {
        return ResponseEntity.ok(purchaseService.createSupplier(request));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseDTO>> getPurchases(@RequestParam Long companyId) {
        return ResponseEntity.ok(purchaseService.findPurchasesByCompany(companyId));
    }

    @PostMapping
    public ResponseEntity<PurchaseDTO> createPurchase(@RequestBody @Valid CreatePurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.createPurchaseDTO(request));
    }
}

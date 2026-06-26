package com.phcpro.modules.purchases.controller;

import com.phcpro.modules.purchases.dto.*;
import com.phcpro.modules.purchases.service.PurchaseOrderService;
import com.phcpro.modules.purchases.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseController(PurchaseService purchaseService, PurchaseOrderService purchaseOrderService) {
        this.purchaseService = purchaseService;
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDTO>> getSuppliers(@RequestParam Long companyId) {
        return ResponseEntity.ok(purchaseService.findSuppliersByCompany(companyId));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierDTO> createSupplier(@RequestBody @Valid CreateSupplierRequest request) {
        return ResponseEntity.ok(purchaseService.createSupplier(request));
    }

    @PutMapping("/suppliers/{id}")
    public ResponseEntity<SupplierDTO> updateSupplier(
            @PathVariable Long id, @RequestBody @Valid CreateSupplierRequest request) {
        return ResponseEntity.ok(purchaseService.updateSupplier(id, request));
    }

    @PatchMapping("/suppliers/{id}/active")
    public ResponseEntity<SupplierDTO> setSupplierActive(
            @PathVariable Long id, @RequestParam Long companyId, @RequestParam boolean value) {
        return ResponseEntity.ok(purchaseService.setSupplierActive(id, companyId, value));
    }

    @GetMapping("/suppliers/search")
    public ResponseEntity<List<SupplierDTO>> searchSuppliers(
            @RequestParam Long companyId, @RequestParam(required = false) String q) {
        return ResponseEntity.ok(purchaseService.searchSuppliers(companyId, q));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseDTO>> getPurchases(@RequestParam Long companyId) {
        return ResponseEntity.ok(purchaseService.findPurchasesByCompany(companyId));
    }

    @PostMapping
    public ResponseEntity<PurchaseDTO> createPurchase(@RequestBody @Valid CreatePurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.createPurchaseDTO(request));
    }

    // --- Encomendas a fornecedor (purchase orders) ---

    @GetMapping("/orders")
    public ResponseEntity<List<PurchaseOrderDTO>> getOrders(
            @RequestParam Long companyId, @RequestParam(required = false) String q) {
        return ResponseEntity.ok(q == null || q.isBlank()
                ? purchaseOrderService.findOrdersByCompany(companyId)
                : purchaseOrderService.searchOrders(companyId, q));
    }

    @PostMapping("/orders")
    public ResponseEntity<PurchaseOrderDTO> createOrder(@RequestBody @Valid CreatePurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.createOrder(request));
    }

    @PostMapping("/orders/{id}/receive")
    public ResponseEntity<PurchaseOrderDTO> receiveOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.receiveOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<PurchaseOrderDTO> cancelOrder(
            @PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(purchaseOrderService.cancelOrder(id, reason));
    }

    // --- Contas a pagar a fornecedor ---

    @GetMapping("/payables")
    public ResponseEntity<List<PayableDTO>> getPayables(@RequestParam Long companyId) {
        return ResponseEntity.ok(purchaseService.findPayablesByCompany(companyId));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PurchaseDTO> paySupplier(
            @PathVariable Long id,
            @RequestParam java.math.BigDecimal amount,
            @RequestParam Long financeAccountId,
            @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(purchaseService.registerSupplierPayment(id, amount, financeAccountId, reference));
    }
}

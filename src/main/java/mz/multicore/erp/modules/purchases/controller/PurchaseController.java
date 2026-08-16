package mz.multicore.erp.modules.purchases.controller;

import mz.multicore.erp.modules.purchases.dto.*;
import mz.multicore.erp.modules.purchases.service.PurchaseOrderService;
import mz.multicore.erp.modules.purchases.service.PurchaseService;
import mz.multicore.erp.modules.purchases.service.ReorderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final PurchaseOrderService purchaseOrderService;
    private final ReorderService reorderService;
    private final mz.multicore.erp.modules.purchases.service.GoodsReceiptDiscrepancyService discrepancyService;

    public PurchaseController(PurchaseService purchaseService, PurchaseOrderService purchaseOrderService,
                             ReorderService reorderService,
                             mz.multicore.erp.modules.purchases.service.GoodsReceiptDiscrepancyService discrepancyService) {
        this.purchaseService = purchaseService;
        this.purchaseOrderService = purchaseOrderService;
        this.reorderService = reorderService;
        this.discrepancyService = discrepancyService;
    }

    /** Reposição automática: produtos abaixo do stock mínimo, com quantidade sugerida (em caixas). */
    @GetMapping("/reorder-suggestions")
    public ResponseEntity<List<ReorderSuggestionDTO>> reorderSuggestions(@RequestParam Long companyId) {
        return ResponseEntity.ok(reorderService.suggestions(companyId));
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

    @PostMapping("/orders/{id}/receive-partial")
    public ResponseEntity<PurchaseOrderDTO> receivePartial(
            @PathVariable Long id, @RequestBody @Valid ReceivePurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.receivePartial(id, request));
    }

    // ─── Conferência à chegada: divergências ────────────────────────────────

    /** Ocorrências de um período. Ver docs/CONFERENCIA_CHEGADA_SPEC.md. */
    @GetMapping("/discrepancies")
    public ResponseEntity<List<mz.multicore.erp.modules.purchases.dto.GoodsReceiptDiscrepancyDTO>> listDiscrepancies(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return ResponseEntity.ok(discrepancyService.list(from, to));
    }

    /** Só o que ainda há a reclamar ao fornecedor. */
    @GetMapping("/discrepancies/open")
    public ResponseEntity<List<mz.multicore.erp.modules.purchases.dto.GoodsReceiptDiscrepancyDTO>> listOpenDiscrepancies() {
        return ResponseEntity.ok(discrepancyService.listOpen());
    }

    /** Resumo por fornecedor, do que mais custou para o que menos custou. */
    @GetMapping("/discrepancies/by-supplier")
    public ResponseEntity<List<mz.multicore.erp.modules.purchases.dto.SupplierDiscrepancyDTO>> discrepanciesBySupplier(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return ResponseEntity.ok(discrepancyService.summaryBySupplier(from, to));
    }

    @PostMapping("/discrepancies/{id}/resolve")
    public ResponseEntity<mz.multicore.erp.modules.purchases.dto.GoodsReceiptDiscrepancyDTO> resolveDiscrepancy(
            @PathVariable Long id,
            @RequestBody mz.multicore.erp.modules.purchases.dto.ResolveDiscrepancyRequest request) {
        return ResponseEntity.ok(discrepancyService.resolve(id, request == null ? null : request.resolutionNotes()));
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

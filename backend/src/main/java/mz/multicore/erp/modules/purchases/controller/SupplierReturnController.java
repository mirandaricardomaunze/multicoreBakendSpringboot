package mz.multicore.erp.modules.purchases.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierReturnRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierReturnDTO;
import mz.multicore.erp.modules.purchases.service.SupplierReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Devoluções de mercadoria ao fornecedor (notas DF). */
@RestController
@RequestMapping("/api/purchases")
public class SupplierReturnController {

    private final SupplierReturnService supplierReturnService;
    private final mz.multicore.erp.modules.purchases.service.SupplierRefundService supplierRefundService;

    public SupplierReturnController(SupplierReturnService supplierReturnService,
                                    mz.multicore.erp.modules.purchases.service.SupplierRefundService supplierRefundService) {
        this.supplierReturnService = supplierReturnService;
        this.supplierRefundService = supplierRefundService;
    }

    /** O fornecedor devolveu dinheiro de uma compra paga e depois devolvida. */
    @PostMapping("/{id}/supplier-refund")
    public ResponseEntity<mz.multicore.erp.modules.purchases.dto.PurchaseDTO> refund(
            @PathVariable Long id, @RequestParam java.math.BigDecimal amount, @RequestParam Long financeAccountId,
            @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(supplierRefundService.register(id, amount, financeAccountId, reference));
    }

    @PostMapping("/{id}/returns")
    public ResponseEntity<SupplierReturnDTO> register(@PathVariable Long id,
                                                      @RequestBody @Valid CreateSupplierReturnRequest request) {
        return ResponseEntity.ok(supplierReturnService.register(id, request));
    }

    @GetMapping("/returns")
    public ResponseEntity<List<SupplierReturnDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(supplierReturnService.findByCompany(companyId));
    }
}

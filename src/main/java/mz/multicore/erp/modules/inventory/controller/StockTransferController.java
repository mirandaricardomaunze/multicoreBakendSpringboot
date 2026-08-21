package mz.multicore.erp.modules.inventory.controller;

import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/transfers")
public class StockTransferController {

    private final StockTransferService stockTransferService;
    private final mz.multicore.erp.modules.comercial.service.InternalReplenishmentService replenishmentService;

    public StockTransferController(StockTransferService stockTransferService,
            mz.multicore.erp.modules.comercial.service.InternalReplenishmentService replenishmentService) {
        this.stockTransferService = stockTransferService;
        this.replenishmentService = replenishmentService;
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

    @PostMapping("/{id}/approve")
    public ResponseEntity<StockTransferDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(stockTransferService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<StockTransferDTO> reject(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("rejectionReason");
        return ResponseEntity.ok(stockTransferService.reject(id, reason));
    }

    /**
     * Regista a encomenda de reposição em falta a partir desta transferência.
     * Não move stock — a mercadoria já mudou de armazém. Ver REPOSICAO_INTERNA_SPEC §5.
     */
    @PostMapping("/{id}/order")
    public ResponseEntity<mz.multicore.erp.modules.comercial.dto.OrderDTO> recordOrder(@PathVariable Long id) {
        return ResponseEntity.ok(replenishmentService.recordOrderFromTransfer(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<StockTransferDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(stockTransferService.cancel(id));
    }
}

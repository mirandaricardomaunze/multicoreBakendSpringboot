package mz.multicore.erp.modules.pos.controller;

import mz.multicore.erp.modules.pos.dto.PosSalePaymentSummaryDTO;
import mz.multicore.erp.modules.pos.service.PosSalePaymentQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pos/sales")
public class PosSalePaymentController {

    private final PosSalePaymentQueryService queryService;

    public PosSalePaymentController(PosSalePaymentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PosSalePaymentSummaryDTO>> getPaymentSummaries(
            @RequestParam(name = "invoiceIds", required = false) List<Long> invoiceIds) {
        return ResponseEntity.ok(queryService.summarize(invoiceIds));
    }
}

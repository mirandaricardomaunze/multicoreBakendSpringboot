package mz.multicore.erp.modules.pos.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest;
import mz.multicore.erp.modules.pos.dto.MobilePaymentResponse;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse;
import mz.multicore.erp.modules.pos.service.MobilePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pos/mobile-payment")
public class MobilePaymentController {

    private final MobilePaymentService mobilePaymentService;

    public MobilePaymentController(MobilePaymentService mobilePaymentService) {
        this.mobilePaymentService = mobilePaymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<MobilePaymentResponse> initiatePayment(@RequestBody @Valid InitiateMobilePaymentRequest request) {
        return ResponseEntity.ok(mobilePaymentService.initiatePayment(request));
    }

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<MobilePaymentStatusResponse> getStatus(
            @PathVariable String transactionId,
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(mobilePaymentService.checkStatus(transactionId, companyId));
    }

    @PostMapping("/{transactionId}/simulate-complete")
    public ResponseEntity<MobilePaymentStatusResponse> simulateComplete(
            @PathVariable String transactionId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "true") boolean approve
    ) {
        return ResponseEntity.ok(mobilePaymentService.simulateComplete(transactionId, companyId, approve));
    }
}

package mz.multicore.erp.modules.subscription.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SelfServiceSubscriptionPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPlanDetailDTO;
import mz.multicore.erp.modules.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de gestão e renovação da própria assinatura da empresa (tenant-scoped).
 */
@RestController
@RequestMapping("/api/subscription")
public class MySubscriptionController {

    private final SubscriptionService subscriptionService;

    public MySubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public MySubscriptionDTO me() {
        return subscriptionService.getMySubscription();
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanDetailDTO>> listPlans() {
        return ResponseEntity.ok(subscriptionService.listAvailablePlans());
    }

    @PostMapping("/renew")
    public ResponseEntity<SubscriptionPaymentResultDTO> renew(
            @RequestBody @Valid SelfServiceSubscriptionPaymentRequest request) {
        return ResponseEntity.ok(subscriptionService.initiateSelfServiceRenewal(request));
    }

    @PostMapping("/confirm-payment/{transactionId}")
    public ResponseEntity<SubscriptionPaymentResultDTO> confirmPayment(@PathVariable String transactionId) {
        return ResponseEntity.ok(subscriptionService.confirmMobileRenewal(transactionId));
    }
}

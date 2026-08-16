package mz.multicore.erp.modules.subscription.controller;

import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.modules.subscription.service.SubscriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vista da própria assinatura para o assinante (tenant-scoped). Requer token + X-Company-Id. */
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
}

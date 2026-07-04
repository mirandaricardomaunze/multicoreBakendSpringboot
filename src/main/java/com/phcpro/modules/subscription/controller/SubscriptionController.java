package com.phcpro.modules.subscription.controller;

import com.phcpro.modules.subscription.dto.RecordPaymentRequest;
import com.phcpro.modules.subscription.dto.SaveSubscriptionRequest;
import com.phcpro.modules.subscription.dto.SubscriptionDTO;
import com.phcpro.modules.subscription.dto.SubscriptionPaymentDTO;
import com.phcpro.modules.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Assinaturas e pagamentos da plataforma. Protegido pelo caminho /api/platform/** (superadmin). */
@RestController
@RequestMapping("/api/platform/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<SubscriptionDTO> overview() {
        return subscriptionService.listOverview();
    }

    @PutMapping("/{companyId}")
    public SubscriptionDTO save(@PathVariable Long companyId, @Valid @RequestBody SaveSubscriptionRequest request) {
        return subscriptionService.saveSubscription(companyId, request);
    }

    @PatchMapping("/{companyId}/status")
    public SubscriptionDTO changeStatus(@PathVariable Long companyId, @RequestBody StatusRequest request) {
        return subscriptionService.changeStatus(companyId, request.status());
    }

    @GetMapping("/{companyId}/payments")
    public List<SubscriptionPaymentDTO> payments(@PathVariable Long companyId) {
        return subscriptionService.listPayments(companyId);
    }

    @PostMapping("/{companyId}/payments")
    public SubscriptionPaymentDTO recordPayment(@PathVariable Long companyId,
                                                @Valid @RequestBody RecordPaymentRequest request) {
        return subscriptionService.recordPayment(companyId, request);
    }

    public record StatusRequest(String status) {}
}

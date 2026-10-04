package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.subscription.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP para a gestão e renovação da própria assinatura da empresa ({@code /api/subscription/*}).
 */
@Component
@Profile("desktop")
public class MySubscriptionApiClient {

    private final DesktopClientFactory clientFactory;

    public MySubscriptionApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public MySubscriptionDTO getMySubscription() {
        return clientFactory.authenticatedClient().get("/api/subscription/me", MySubscriptionDTO.class);
    }

    public List<SubscriptionPlanDetailDTO> listPlans() {
        return clientFactory.authenticatedClient().getList("/api/subscription/plans", SubscriptionPlanDetailDTO.class);
    }

    public SubscriptionPaymentResultDTO renewSubscription(SelfServiceSubscriptionPaymentRequest request) {
        return clientFactory.authenticatedClient().post("/api/subscription/renew", request, SubscriptionPaymentResultDTO.class);
    }

    public SubscriptionPaymentResultDTO confirmPayment(String transactionId) {
        return clientFactory.authenticatedClient().post("/api/subscription/confirm-payment/" + enc(transactionId),
                null, SubscriptionPaymentResultDTO.class);
    }

    public POSApiClient getPosApiClient() {
        return new POSApiClient(clientFactory);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}

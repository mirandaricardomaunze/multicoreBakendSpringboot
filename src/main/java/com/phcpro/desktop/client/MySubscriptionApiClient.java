package com.phcpro.desktop.client;

import com.phcpro.modules.subscription.dto.MySubscriptionDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Cliente HTTP para a vista da própria assinatura da empresa ({@code /api/subscription/me}). */
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
}

package com.phcpro.desktop.client;

import com.phcpro.modules.promotions.dto.CreatePromotionRequest;
import com.phcpro.modules.promotions.dto.PromotionDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP para as promoções de loja ({@code /api/promotions}). Espelha o padrão do
 * {@link ComercialApiClient}: métodos tipados sobre o {@link DesktopClientFactory}.
 */
@Component
@Profile("desktop")
public class PromotionApiClient {

    private final DesktopClientFactory clientFactory;

    public PromotionApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<PromotionDTO> findByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/promotions?companyId=" + companyId, PromotionDTO.class);
    }

    public PromotionDTO createPromotion(CreatePromotionRequest request) {
        return clientFactory.authenticatedClient().post("/api/promotions", request, PromotionDTO.class);
    }

    public PromotionDTO setActive(Long id, boolean active) {
        return clientFactory.authenticatedClient()
                .post("/api/promotions/" + id + "/active?active=" + active, null, PromotionDTO.class);
    }
}

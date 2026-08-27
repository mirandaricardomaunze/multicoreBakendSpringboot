package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.promotions.dto.AppliedPromotionDTO;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

    /** Melhor promoção aplicável a uma linha (produto/categoria/quantidade); vazio se nenhuma. */
    public Optional<AppliedPromotionDTO> bestPromotion(Long companyId, Long productId, Long categoryId,
                                                       BigDecimal quantity) {
        String path = "/api/promotions/best?companyId=" + companyId + "&productId=" + productId
                + "&quantity=" + quantity;
        if (categoryId != null) path += "&categoryId=" + categoryId;
        return Optional.ofNullable(
                clientFactory.authenticatedClient().get(path, AppliedPromotionDTO.class));
    }
}

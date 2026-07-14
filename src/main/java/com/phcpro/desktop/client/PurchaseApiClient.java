package com.phcpro.desktop.client;

import com.phcpro.modules.purchases.dto.PurchaseDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP para as compras ({@code /api/purchases}). Espelha o padrão do
 * {@link ComercialApiClient}. Cobre por agora a leitura usada pelo dashboard; será estendido
 * quando o ComprasPanel migrar.
 */
@Component
@Profile("desktop")
public class PurchaseApiClient {

    private final DesktopClientFactory clientFactory;

    public PurchaseApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<PurchaseDTO> getPurchasesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/purchases?companyId=" + companyId, PurchaseDTO.class);
    }
}

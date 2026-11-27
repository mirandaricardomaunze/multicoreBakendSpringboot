package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.dto.CreatedApiKeyDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HTTP client for the public API keys of the active company ({@code /api/integrations/api-keys},
 * docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1.1). The desktop never generates nor stores a secret: it only shows the
 * one returned by {@link #create}.
 */
@Component
@Profile("desktop")
public class ApiKeyApiClient {

    private static final String BASE = "/api/integrations/api-keys";

    private final DesktopClientFactory clientFactory;

    public ApiKeyApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ApiKeyDTO> list() {
        return clientFactory.authenticatedClient().getList(BASE, ApiKeyDTO.class);
    }

    public CreatedApiKeyDTO create(CreateApiKeyRequest request) {
        return clientFactory.authenticatedClient().post(BASE, request, CreatedApiKeyDTO.class);
    }

    public ApiKeyDTO revoke(Long id) {
        return clientFactory.authenticatedClient().post(BASE + "/" + id + "/revoke", null, ApiKeyDTO.class);
    }
}

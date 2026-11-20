package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.changes.ChangeVersionDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Reads the active company's change counter (see docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §3). */
@Component
@Profile("desktop")
public class ChangeFeedApiClient {

    private final DesktopClientFactory clientFactory;

    public ChangeFeedApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ChangeVersionDTO current() {
        return clientFactory.authenticatedClient().get("/api/changes", ChangeVersionDTO.class);
    }
}

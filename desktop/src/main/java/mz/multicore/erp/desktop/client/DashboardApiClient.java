package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Cliente HTTP Desktop para o cockpit executivo do Dashboard (/api/dashboard/operational-pulse).
 */
@Component
@Profile("desktop")
public class DashboardApiClient {

    private final DesktopClientFactory clientFactory;

    public DashboardApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public OperationalPulseDTO getOperationalPulse() {
        return clientFactory.authenticatedClient().get(
                "/api/dashboard/operational-pulse",
                OperationalPulseDTO.class
        );
    }
}

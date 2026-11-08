package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.activity.dto.ActivityCatalogDTO;
import mz.multicore.erp.modules.activity.dto.CompanyActivityDTO;
import mz.multicore.erp.modules.activity.dto.UpdateCompanyActivityRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** HTTP client for the company activity profile ({@code /api/activity}, SA-C01). */
@Component
@Profile("desktop")
public class ActivityApiClient {

    private final DesktopClientFactory clientFactory;

    public ActivityApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ActivityCatalogDTO catalog() {
        return clientFactory.authenticatedClient().get("/api/activity/catalog", ActivityCatalogDTO.class);
    }

    public CompanyActivityDTO company(Long companyId) {
        return clientFactory.authenticatedClient().get("/api/activity/company?companyId=" + companyId,
                CompanyActivityDTO.class);
    }

    public CompanyActivityDTO update(Long companyId, List<String> profiles) {
        return clientFactory.authenticatedClient().put("/api/activity/company",
                new UpdateCompanyActivityRequest(companyId, profiles), CompanyActivityDTO.class);
    }
}

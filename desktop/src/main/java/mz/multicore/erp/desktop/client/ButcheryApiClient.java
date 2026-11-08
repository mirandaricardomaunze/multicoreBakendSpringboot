package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.butchery.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** HTTP client for the butchery carcass breakdown ({@code /api/butchery}, F3). */
@Component
@Profile("desktop")
public class ButcheryApiClient {

    private final DesktopClientFactory clientFactory;

    public ButcheryApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<BreakdownTemplateDTO> templates(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/butchery/templates?companyId=" + companyId, BreakdownTemplateDTO.class);
    }

    public BreakdownTemplateDTO saveTemplate(SaveBreakdownTemplateRequest request) {
        return clientFactory.authenticatedClient().put("/api/butchery/templates", request, BreakdownTemplateDTO.class);
    }

    public List<CarcassBreakdownDTO> breakdowns(Long companyId, int days) {
        return clientFactory.authenticatedClient().getList("/api/butchery/breakdowns?companyId=" + companyId + "&days=" + days,
                CarcassBreakdownDTO.class);
    }

    public CarcassBreakdownDTO post(PostBreakdownRequest request) {
        return clientFactory.authenticatedClient().post("/api/butchery/breakdowns", request, CarcassBreakdownDTO.class);
    }

    public CarcassBreakdownDTO cancel(Long companyId, Long id, String reason) {
        return clientFactory.authenticatedClient().post("/api/butchery/breakdowns/" + id + "/cancel",
                new CancelBreakdownRequest(companyId, reason), CarcassBreakdownDTO.class);
    }
}

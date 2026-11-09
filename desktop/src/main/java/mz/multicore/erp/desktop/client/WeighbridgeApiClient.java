package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.weighbridge.dto.FirstWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.SecondWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.WeighingTicketDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** HTTP client for the truck scale ({@code /api/weighbridge}, SA-C18). */
@Component
@Profile("desktop")
public class WeighbridgeApiClient {

    private final DesktopClientFactory clientFactory;

    public WeighbridgeApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<WeighingTicketDTO> tickets(Long companyId, int days) {
        return clientFactory.authenticatedClient().getList("/api/weighbridge/tickets?companyId=" + companyId + "&days=" + days,
                WeighingTicketDTO.class);
    }

    public WeighingTicketDTO first(FirstWeighingRequest request) {
        return clientFactory.authenticatedClient().post("/api/weighbridge/tickets", request, WeighingTicketDTO.class);
    }

    public WeighingTicketDTO second(Long id, SecondWeighingRequest request) {
        return clientFactory.authenticatedClient().post("/api/weighbridge/tickets/" + id + "/second", request, WeighingTicketDTO.class);
    }

    public byte[] pdf(Long companyId, Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/weighbridge/tickets/" + id + "/pdf?companyId=" + companyId);
    }
}

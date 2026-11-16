package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.consignment.dto.ConsignmentDTO;
import mz.multicore.erp.modules.consignment.dto.DeliverConsignmentRequest;
import mz.multicore.erp.modules.consignment.dto.SettleConsignmentRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** HTTP client for consignment deliveries to resellers ({@code /api/consignments}, SA-C11). */
@Component
@Profile("desktop")
public class ConsignmentApiClient {

    private final DesktopClientFactory clientFactory;

    public ConsignmentApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ConsignmentDTO> list(Long companyId, int days) {
        return clientFactory.authenticatedClient().getList("/api/consignments?companyId=" + companyId + "&days=" + days, ConsignmentDTO.class);
    }

    public ConsignmentDTO deliver(DeliverConsignmentRequest request) {
        return clientFactory.authenticatedClient().post("/api/consignments", request, ConsignmentDTO.class);
    }

    public ConsignmentDTO settle(Long id, SettleConsignmentRequest request) {
        return clientFactory.authenticatedClient().post("/api/consignments/" + id + "/settle", request, ConsignmentDTO.class);
    }
}

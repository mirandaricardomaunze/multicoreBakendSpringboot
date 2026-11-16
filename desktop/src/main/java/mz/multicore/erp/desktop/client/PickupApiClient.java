package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.pickup.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** HTTP client for paid material awaiting pickup ({@code /api/pickups}, SA-C07). */
@Component
@Profile("desktop")
public class PickupApiClient {

    private final DesktopClientFactory clientFactory;

    public PickupApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<PickupAccountSummaryDTO> accounts(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/pickups/accounts?companyId=" + companyId,
                PickupAccountSummaryDTO.class);
    }

    public PickupAccountDTO account(Long companyId, Long accountId) {
        return clientFactory.authenticatedClient().get("/api/pickups/accounts/" + accountId + "?companyId=" + companyId,
                PickupAccountDTO.class);
    }

    public PickupAccountDTO open(OpenPickupAccountRequest request) {
        return clientFactory.authenticatedClient().post("/api/pickups/accounts", request, PickupAccountDTO.class);
    }

    public List<EligibleInvoiceDTO> eligibleInvoices(Long companyId, String search) {
        String query = search == null || search.isBlank() ? "" : "&search=" + URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient().getList("/api/pickups/eligible-invoices?companyId=" + companyId + query,
                EligibleInvoiceDTO.class);
    }

    public List<PendingPickupStockDTO> pendingStock(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/pickups/pending-stock?companyId=" + companyId,
                PendingPickupStockDTO.class);
    }

    public MaterialPickupDTO issue(IssuePickupRequest request) {
        return clientFactory.authenticatedClient().post("/api/pickups/slips", request, MaterialPickupDTO.class);
    }

    public MaterialPickupDTO cancel(Long companyId, Long pickupId, String reason) {
        return clientFactory.authenticatedClient().post("/api/pickups/slips/" + pickupId + "/cancel",
                new CancelPickupRequest(companyId, reason), MaterialPickupDTO.class);
    }

    public byte[] slipPdf(Long companyId, Long pickupId) {
        return clientFactory.authenticatedClient().getBytes("/api/pickups/slips/" + pickupId + "/pdf?companyId=" + companyId);
    }
}

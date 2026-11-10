package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.advances.dto.ApplyAdvanceRequest;
import mz.multicore.erp.modules.advances.dto.CustomerAdvanceDTO;
import mz.multicore.erp.modules.advances.dto.ReceiveAdvanceRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** HTTP client for customer advances ({@code /api/advances}, SA-C10). */
@Component
@Profile("desktop")
public class AdvancesApiClient {

    private final DesktopClientFactory clientFactory;

    public AdvancesApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<CustomerAdvanceDTO> list(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/advances?companyId=" + companyId, CustomerAdvanceDTO.class);
    }

    public CustomerAdvanceDTO receive(ReceiveAdvanceRequest request) {
        return clientFactory.authenticatedClient().post("/api/advances", request, CustomerAdvanceDTO.class);
    }

    public byte[] receiptPdf(Long receiptId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/receipt/document/" + receiptId);
    }

    public CustomerAdvanceDTO refund(Long companyId, Long advanceId, String reason) {
        return clientFactory.authenticatedClient().post("/api/advances/" + advanceId + "/refund",
                new mz.multicore.erp.modules.advances.dto.RefundAdvanceRequest(companyId, reason), CustomerAdvanceDTO.class);
    }

    public CustomerAdvanceDTO apply(Long companyId, Long advanceId, Long invoiceId) {
        return clientFactory.authenticatedClient().post("/api/advances/" + advanceId + "/apply",
                new ApplyAdvanceRequest(companyId, invoiceId), CustomerAdvanceDTO.class);
    }
}

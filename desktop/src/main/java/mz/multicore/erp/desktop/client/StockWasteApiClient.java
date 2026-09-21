package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("desktop")
public class StockWasteApiClient {

    private final DesktopClientFactory clientFactory;

    public StockWasteApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<StockWasteDTO> findByCompany(Long companyId, WasteStatus status) {
        String path = "/api/inventory/waste?companyId=" + companyId;
        if (status != null) {
            path += "&status=" + status.name();
        }
        return clientFactory.authenticatedClient().getList(path, StockWasteDTO.class);
    }

    public StockWasteDTO register(CreateStockWasteRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/waste", request, StockWasteDTO.class);
    }

    public StockWasteDTO approve(Long id, boolean approved, String notes) {
        ApproveWasteRequest req = new ApproveWasteRequest(approved, notes);
        return clientFactory.authenticatedClient()
                .post("/api/inventory/waste/" + id + "/approve", req, StockWasteDTO.class);
    }

    public List<ExpiringBatchAlertDTO> getExpiringRadar(Long companyId, int days) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/waste/radar?companyId=" + companyId + "&days=" + days, ExpiringBatchAlertDTO.class);
    }

    public WasteSummaryDTO getSummary(Long companyId, LocalDate start, LocalDate end) {
        return clientFactory.authenticatedClient()
                .get("/api/inventory/waste/summary?companyId=" + companyId + "&start=" + start + "&end=" + end, WasteSummaryDTO.class);
    }

    public byte[] renderReportPdf(Long companyId, LocalDate start, LocalDate end) {
        String path = "/api/inventory/waste/report/pdf?companyId=" + companyId;
        if (start != null) path += "&start=" + start;
        if (end != null) path += "&end=" + end;
        return clientFactory.authenticatedClient().getBytes(path);
    }
}

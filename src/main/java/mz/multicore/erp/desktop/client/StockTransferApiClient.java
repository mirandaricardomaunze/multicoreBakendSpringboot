package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** Cliente HTTP para transferências entre armazéns ({@code /api/inventory/transfers}). */
@Component
@Profile("desktop")
public class StockTransferApiClient {

    private final DesktopClientFactory clientFactory;

    public StockTransferApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<StockTransferDTO> findByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/transfers?companyId=" + companyId, StockTransferDTO.class);
    }

    public StockTransferDTO create(CreateStockTransferRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/transfers", request, StockTransferDTO.class);
    }

    public StockTransferDTO approve(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/transfers/" + id + "/approve", null, StockTransferDTO.class);
    }

    public StockTransferDTO reject(Long id, String reason) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/transfers/" + id + "/reject", new RejectRequest(reason), StockTransferDTO.class);
    }

    /** PDF da guia de transferência ({@code /api/print/stock-transfer/{id}}). */
    public byte[] renderTransfer(Long transferId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/stock-transfer/" + transferId);
    }

    record RejectRequest(String rejectionReason) {}
}

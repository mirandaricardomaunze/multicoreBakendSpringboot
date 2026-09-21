package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.inventory.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("desktop")
public class InventoryPhysicalCountingApiClient {

    private final DesktopClientFactory clientFactory;

    public InventoryPhysicalCountingApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<InventorySessionDTO> listSessions() {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/physical-counting", InventorySessionDTO.class);
    }

    public InventorySessionDTO getSessionById(Long id) {
        return clientFactory.authenticatedClient()
                .get("/api/inventory/physical-counting/" + id, InventorySessionDTO.class);
    }

    public InventorySessionDTO createSession(CreateInventorySessionRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/physical-counting", request, InventorySessionDTO.class);
    }

    public InventorySessionDTO startCounting(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/physical-counting/" + id + "/start", null, InventorySessionDTO.class);
    }

    public InventorySessionDTO recordCount(Long id, UpdateInventoryItemCountRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/physical-counting/" + id + "/count", request, InventorySessionDTO.class);
    }

    public InventorySessionDTO closeAndAdjustStock(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/physical-counting/" + id + "/close", null, InventorySessionDTO.class);
    }

    public InventorySessionDTO cancelSession(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/physical-counting/" + id + "/cancel", null, InventorySessionDTO.class);
    }

    public byte[] renderPdf(Long id) {
        return clientFactory.authenticatedClient()
                .getBytes("/api/inventory/physical-counting/" + id + "/pdf");
    }
}

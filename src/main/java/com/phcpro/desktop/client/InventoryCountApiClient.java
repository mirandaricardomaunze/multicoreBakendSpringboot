package com.phcpro.desktop.client;

import com.phcpro.modules.inventory.dto.CreateInventoryCountRequest;
import com.phcpro.modules.inventory.dto.InventoryCountDTO;
import com.phcpro.modules.inventory.dto.SaveCountsRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Cliente HTTP para sessões de contagem de inventário ({@code /api/inventory/counts}). */
@Component
@Profile("desktop")
public class InventoryCountApiClient {

    private final DesktopClientFactory clientFactory;

    public InventoryCountApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<InventoryCountDTO> listSessions(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/counts?companyId=" + companyId, InventoryCountDTO.class);
    }

    public InventoryCountDTO getSession(Long id) {
        return clientFactory.authenticatedClient()
                .get("/api/inventory/counts/" + id, InventoryCountDTO.class);
    }

    public InventoryCountDTO createSession(Long warehouseId, String note) {
        return clientFactory.authenticatedClient().post("/api/inventory/counts",
                new CreateInventoryCountRequest(warehouseId, note), InventoryCountDTO.class);
    }

    public InventoryCountDTO saveCounts(Long sessionId, Map<Long, BigDecimal> counts) {
        List<SaveCountsRequest.CountEntry> entries = counts.entrySet().stream()
                .map(e -> new SaveCountsRequest.CountEntry(e.getKey(), e.getValue()))
                .toList();
        return clientFactory.authenticatedClient().put("/api/inventory/counts/" + sessionId + "/counts",
                new SaveCountsRequest(entries), InventoryCountDTO.class);
    }

    public InventoryCountDTO applySession(Long sessionId) {
        return clientFactory.authenticatedClient()
                .post("/api/inventory/counts/" + sessionId + "/apply", null, InventoryCountDTO.class);
    }

    public void cancelSession(Long sessionId) {
        clientFactory.authenticatedClient().post("/api/inventory/counts/" + sessionId + "/cancel", null);
    }
}

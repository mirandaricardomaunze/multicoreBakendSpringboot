package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.customorders.dto.*;
import mz.multicore.erp.modules.customorders.model.CustomOrderStage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** HTTP client for orders with a date and personalisation ({@code /api/custom-orders}, SA-C13). */
@Component
@Profile("desktop")
public class CustomOrdersApiClient {

    private final DesktopClientFactory clientFactory;

    public CustomOrdersApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<CustomOrderDTO> agenda(Long companyId, LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient().getList("/api/custom-orders/agenda?companyId=" + companyId + "&from=" + from
                + "&to=" + to, CustomOrderDTO.class);
    }

    public CustomOrderDTO save(Long orderId, SaveCustomOrderRequest request) {
        return clientFactory.authenticatedClient().put("/api/custom-orders/" + orderId, request, CustomOrderDTO.class);
    }

    public CustomOrderDTO stage(Long companyId, Long orderId, CustomOrderStage stage) {
        return clientFactory.authenticatedClient().post("/api/custom-orders/" + orderId + "/stage",
                new CustomOrderStageRequest(companyId, stage), CustomOrderDTO.class);
    }

    public SendToPlanResultDTO sendToPlan(Long companyId, Long orderId) {
        return clientFactory.authenticatedClient().post("/api/custom-orders/" + orderId + "/send-to-plan?companyId=" + companyId, null,
                SendToPlanResultDTO.class);
    }

    public byte[] image(Long companyId, Long orderId) {
        return clientFactory.authenticatedClient().getBytes("/api/custom-orders/" + orderId + "/image?companyId=" + companyId);
    }

    public byte[] sheet(Long companyId, Long orderId) {
        return clientFactory.authenticatedClient().getBytes("/api/custom-orders/" + orderId + "/sheet?companyId=" + companyId);
    }
}

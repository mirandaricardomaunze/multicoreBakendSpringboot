package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.ConvertOrderToTransferRequest;
import mz.multicore.erp.modules.comercial.dto.CreateDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.CreateStandaloneDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP especializado para guias de remessa (GR) e transferências de reposição.
 */
@Component
@Profile("desktop")
public class CommercialDeliveryGuideApiClient {

    private final DesktopClientFactory clientFactory;

    public CommercialDeliveryGuideApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<DeliveryGuideDTO> getDeliveryGuidesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/delivery-guides?companyId=" + companyId, DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO getDeliveryGuideById(Long id) {
        return clientFactory.authenticatedClient()
                .get("/api/comercial/delivery-guides/" + id, DeliveryGuideDTO.class);
    }

    public StockTransferDTO convertOrderToTransfer(
            Long orderId, String responsible, String vehicle, String notes, String driverName, String vehiclePlate) {
        return clientFactory.authenticatedClient().post("/api/comercial/orders/" + orderId + "/transfer",
                new ConvertOrderToTransferRequest(responsible, vehicle, notes, driverName, vehiclePlate),
                StockTransferDTO.class);
    }

    public DeliveryGuideDTO createDeliveryGuide(Long orderId, String responsible, String vehicle, String notes) {
        return clientFactory.authenticatedClient().post("/api/comercial/delivery-guides",
                new CreateDeliveryGuideRequest(orderId, responsible, vehicle, notes), DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO createStandaloneDeliveryGuide(CreateStandaloneDeliveryGuideRequest request) {
        return clientFactory.authenticatedClient().post("/api/comercial/delivery-guides/standalone",
                request, DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO approveDeliveryGuide(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/delivery-guides/" + id + "/approve", null, DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO rejectDeliveryGuide(Long id, String reason) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/delivery-guides/" + id + "/reject", new RejectGuideRequest(reason), DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO cancelDeliveryGuide(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/delivery-guides/" + id + "/cancel", null, DeliveryGuideDTO.class);
    }

    public byte[] renderDeliveryGuide(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/delivery-guide/" + id);
    }

    public record RejectGuideRequest(String rejectionReason) {}
}

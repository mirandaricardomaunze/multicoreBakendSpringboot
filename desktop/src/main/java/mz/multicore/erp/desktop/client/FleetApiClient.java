package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;
import mz.multicore.erp.modules.fleet.dto.FuelLogDTO;
import mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP Desktop para o módulo de Gestão de Frotas (/api/fleet).
 */
@Component
@Profile("desktop")
public class FleetApiClient {

    private final DesktopClientFactory clientFactory;

    public FleetApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<VehicleDTO> listVehicles() {
        return clientFactory.authenticatedClient().getList("/api/fleet/vehicles", VehicleDTO.class);
    }

    public VehicleDTO getVehicle(Long id) {
        return clientFactory.authenticatedClient().get("/api/fleet/vehicles/" + id, VehicleDTO.class);
    }

    public VehicleDTO createVehicle(CreateVehicleRequest request) {
        return clientFactory.authenticatedClient().post("/api/fleet/vehicles", request, VehicleDTO.class);
    }

    public FuelLogDTO recordFuel(RecordFuelLogRequest request) {
        return clientFactory.authenticatedClient().post("/api/fleet/fuel", request, FuelLogDTO.class);
    }

    public List<FuelLogDTO> listFuelLogs(Long vehicleId) {
        return clientFactory.authenticatedClient().getList("/api/fleet/vehicles/" + vehicleId + "/fuel", FuelLogDTO.class);
    }

    public VehicleDTO updateStatus(Long vehicleId, VehicleStatus status) {
        return clientFactory.authenticatedClient().patch("/api/fleet/vehicles/" + vehicleId + "/status?status=" + status.name(), null, VehicleDTO.class);
    }

    public FleetManifestDTO createManifest(CreateFleetManifestRequest request) {
        return clientFactory.authenticatedClient().post("/api/fleet/manifests", request, FleetManifestDTO.class);
    }

    public List<FleetManifestDTO> listManifests() {
        return clientFactory.authenticatedClient().getList("/api/fleet/manifests", FleetManifestDTO.class);
    }

    public FleetManifestDTO getManifest(Long id) {
        return clientFactory.authenticatedClient().get("/api/fleet/manifests/" + id, FleetManifestDTO.class);
    }

    public FleetManifestDTO updateManifestStatus(Long id, String status) {
        return clientFactory.authenticatedClient().patch("/api/fleet/manifests/" + id + "/status?status=" + status, null, FleetManifestDTO.class);
    }

    public byte[] printManifest(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/fleet/manifests/" + id + "/pdf");
    }

    public List<mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO> listDeliveryGuides() {
        return clientFactory.authenticatedClient().getList("/api/comercial/delivery-guides", mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO.class);
    }
}

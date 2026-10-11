package mz.multicore.erp.modules.fleet.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest;
import mz.multicore.erp.modules.fleet.dto.FuelLogDTO;
import mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.service.FleetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fleet")
public class FleetController {

    private final FleetService fleetService;

    public FleetController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleDTO>> listVehicles() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(fleetService.listVehicles(companyId));
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<VehicleDTO> getVehicle(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(fleetService.getVehicle(companyId, id));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleDTO> createVehicle(@RequestBody @Valid CreateVehicleRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(fleetService.createVehicle(companyId, request));
    }

    @PostMapping("/fuel")
    public ResponseEntity<FuelLogDTO> recordFuel(@RequestBody @Valid RecordFuelLogRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(fleetService.recordFuel(companyId, request));
    }

    @GetMapping("/vehicles/{id}/fuel")
    public ResponseEntity<List<FuelLogDTO>> listFuelLogs(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(fleetService.listFuelLogs(companyId, id));
    }

    @PatchMapping("/vehicles/{id}/status")
    public ResponseEntity<VehicleDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam VehicleStatus status) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(fleetService.updateStatus(companyId, id, status));
    }
}

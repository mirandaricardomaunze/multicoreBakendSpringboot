package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest;
import mz.multicore.erp.modules.fleet.dto.FuelLogDTO;
import mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FleetService {

    private final FleetVehicleManagementService vehicleManagementService;
    private final FleetFuelLogService fuelLogService;

    @Autowired
    public FleetService(FleetVehicleManagementService vehicleManagementService,
                        FleetFuelLogService fuelLogService) {
        this.vehicleManagementService = vehicleManagementService;
        this.fuelLogService = fuelLogService;
    }

    public FleetService(VehicleRepository vehicleRepository,
                        FuelLogRepository fuelLogRepository,
                        CompanyLookup companyLookup) {
        this(vehicleRepository, fuelLogRepository, companyLookup, null);
    }

    public FleetService(VehicleRepository vehicleRepository,
                        FuelLogRepository fuelLogRepository,
                        CompanyLookup companyLookup,
                        ApplicationEventPublisher eventPublisher) {
        this(new FleetVehicleManagementService(vehicleRepository, companyLookup),
             new FleetFuelLogService(vehicleRepository, fuelLogRepository, eventPublisher));
    }

    @Transactional
    public VehicleDTO createVehicle(Long companyId, CreateVehicleRequest request) {
        return vehicleManagementService.createVehicle(companyId, request);
    }

    @Transactional(readOnly = true)
    public List<VehicleDTO> listVehicles(Long companyId) {
        return vehicleManagementService.listVehicles(companyId);
    }

    @Transactional(readOnly = true)
    public VehicleDTO getVehicle(Long companyId, Long id) {
        return vehicleManagementService.getVehicle(companyId, id);
    }

    @Transactional
    public FuelLogDTO recordFuel(Long companyId, RecordFuelLogRequest request) {
        return fuelLogService.recordFuel(companyId, request);
    }

    @Transactional(readOnly = true)
    public List<FuelLogDTO> listFuelLogs(Long companyId, Long vehicleId) {
        return fuelLogService.listFuelLogs(companyId, vehicleId);
    }

    @Transactional
    public VehicleDTO updateStatus(Long companyId, Long vehicleId, VehicleStatus status) {
        return vehicleManagementService.updateStatus(companyId, vehicleId, status);
    }
}

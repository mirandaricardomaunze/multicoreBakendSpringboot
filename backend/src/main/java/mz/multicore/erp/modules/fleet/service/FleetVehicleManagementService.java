package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FleetVehicleManagementService {

    private final VehicleRepository vehicleRepository;
    private final CompanyRepository companyRepository;

    public FleetVehicleManagementService(VehicleRepository vehicleRepository,
                                         CompanyRepository companyRepository) {
        this.vehicleRepository = vehicleRepository;
        this.companyRepository = companyRepository;
    }

    public VehicleDTO createVehicle(Long companyId, CreateVehicleRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        String plate = request.licensePlate().trim().toUpperCase();

        if (vehicleRepository.existsByCompanyIdAndLicensePlate(companyId, plate)) {
            throw new BusinessRuleException("Já existe uma viatura registada com a matrícula '" + plate + "'.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + companyId));

        Vehicle vehicle = new Vehicle();
        vehicle.setCompany(company);
        vehicle.setLicensePlate(plate);
        vehicle.setMake(request.make().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setManufacturingYear(request.manufacturingYear());
        vehicle.setFuelType(request.fuelType());
        vehicle.setStatus(VehicleStatus.OPERATIONAL);
        vehicle.setCurrentOdometerKm(request.initialOdometerKm() != null ? request.initialOdometerKm() : BigDecimal.ZERO);
        vehicle.setAssignedDriverName(request.assignedDriverName() != null ? request.assignedDriverName().trim() : null);
        vehicle.setInsuranceExpiryDate(request.insuranceExpiryDate());
        vehicle.setInspectionExpiryDate(request.inspectionExpiryDate());
        vehicle.setAverageFuelConsumptionKmPerLiter(BigDecimal.ZERO);

        Vehicle saved = vehicleRepository.save(vehicle);
        return toVehicleDTO(saved);
    }

    public List<VehicleDTO> listVehicles(Long companyId) {
        if (companyId == null) return List.of();
        return vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(companyId)
                .stream()
                .map(this::toVehicleDTO)
                .toList();
    }

    public VehicleDTO getVehicle(Long companyId, Long id) {
        Vehicle v = vehicleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Viatura não encontrada ou não pertence a esta empresa."));
        return toVehicleDTO(v);
    }

    public VehicleDTO updateStatus(Long companyId, Long vehicleId, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findByIdAndCompanyId(vehicleId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Viatura não encontrada."));
        vehicle.setStatus(status);
        return toVehicleDTO(vehicleRepository.save(vehicle));
    }

    public VehicleDTO toVehicleDTO(Vehicle v) {
        return new VehicleDTO(
                v.getId(),
                v.getCompany().getId(),
                v.getLicensePlate(),
                v.getMake(),
                v.getModel(),
                v.getManufacturingYear(),
                v.getFuelType(),
                v.getStatus(),
                v.getCurrentOdometerKm(),
                v.getAssignedDriverName(),
                v.getInsuranceExpiryDate(),
                v.getInspectionExpiryDate(),
                v.getAverageFuelConsumptionKmPerLiter()
        );
    }
}

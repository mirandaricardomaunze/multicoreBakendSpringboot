package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest;
import mz.multicore.erp.modules.fleet.dto.FuelLogDTO;
import mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;
import mz.multicore.erp.modules.fleet.model.FuelLog;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FleetService {

    private final VehicleRepository vehicleRepository;
    private final FuelLogRepository fuelLogRepository;
    private final CompanyRepository companyRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public FleetService(VehicleRepository vehicleRepository,
                        FuelLogRepository fuelLogRepository,
                        CompanyRepository companyRepository) {
        this(vehicleRepository, fuelLogRepository, companyRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FleetService(VehicleRepository vehicleRepository,
                        FuelLogRepository fuelLogRepository,
                        CompanyRepository companyRepository,
                        org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.vehicleRepository = vehicleRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.companyRepository = companyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public List<VehicleDTO> listVehicles(Long companyId) {
        if (companyId == null) return List.of();
        return vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(companyId)
                .stream()
                .map(this::toVehicleDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleDTO getVehicle(Long companyId, Long id) {
        Vehicle v = vehicleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Viatura não encontrada ou não pertence a esta empresa."));
        return toVehicleDTO(v);
    }

    @Transactional
    public FuelLogDTO recordFuel(Long companyId, RecordFuelLogRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");

        Vehicle vehicle = vehicleRepository.findByIdAndCompanyId(request.vehicleId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Viatura não encontrada."));

        if (request.odometerKm().compareTo(vehicle.getCurrentOdometerKm()) < 0) {
            throw new BusinessRuleException("O odómetro indicado (" + request.odometerKm() + " Km) não pode ser inferior ao odómetro actual da viatura (" + vehicle.getCurrentOdometerKm() + " Km).");
        }

        BigDecimal distanceTraveled = request.odometerKm().subtract(vehicle.getCurrentOdometerKm());
        BigDecimal consumptionRate = BigDecimal.ZERO;
        if (distanceTraveled.compareTo(BigDecimal.ZERO) > 0 && request.litersFilled().compareTo(BigDecimal.ZERO) > 0) {
            consumptionRate = distanceTraveled.divide(request.litersFilled(), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalCost = request.litersFilled().multiply(request.pricePerLiter()).setScale(2, RoundingMode.HALF_UP);

        FuelLog log = new FuelLog();
        log.setVehicle(vehicle);
        log.setLogTimestamp(request.logTimestamp() != null ? request.logTimestamp() : LocalDateTime.now());
        log.setOdometerKm(request.odometerKm());
        log.setLitersFilled(request.litersFilled());
        log.setPricePerLiter(request.pricePerLiter());
        log.setTotalCost(totalCost);
        log.setGasStationName(request.gasStationName());
        log.setDriverName(request.driverName());
        log.setDistanceTraveledSinceLastFillKm(distanceTraveled);
        log.setConsumptionRateKmPerLiter(consumptionRate);

        // Actualiza odómetro e média de consumo na viatura
        vehicle.setCurrentOdometerKm(request.odometerKm());
        if (consumptionRate.compareTo(BigDecimal.ZERO) > 0) {
            if (vehicle.getAverageFuelConsumptionKmPerLiter().compareTo(BigDecimal.ZERO) == 0) {
                vehicle.setAverageFuelConsumptionKmPerLiter(consumptionRate);
            } else {
                BigDecimal updatedAvg = vehicle.getAverageFuelConsumptionKmPerLiter().add(consumptionRate).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                vehicle.setAverageFuelConsumptionKmPerLiter(updatedAvg);
            }
        }

        vehicleRepository.save(vehicle);
        FuelLog saved = fuelLogRepository.save(log);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.FleetFuelLoggedEvent(
                    companyId,
                    saved.getId(),
                    vehicle.getLicensePlate(),
                    saved.getLogTimestamp().toLocalDate(),
                    saved.getTotalCost(),
                    true
            ));
        }

        return toFuelDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<FuelLogDTO> listFuelLogs(Long companyId, Long vehicleId) {
        vehicleRepository.findByIdAndCompanyId(vehicleId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Viatura não encontrada."));

        return fuelLogRepository.findByVehicleIdOrderByLogTimestampDesc(vehicleId)
                .stream()
                .map(this::toFuelDTO)
                .toList();
    }

    @Transactional
    public VehicleDTO updateStatus(Long companyId, Long vehicleId, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findByIdAndCompanyId(vehicleId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Viatura não encontrada."));
        vehicle.setStatus(status);
        return toVehicleDTO(vehicleRepository.save(vehicle));
    }

    private VehicleDTO toVehicleDTO(Vehicle v) {
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

    private FuelLogDTO toFuelDTO(FuelLog l) {
        return new FuelLogDTO(
                l.getId(),
                l.getVehicle().getId(),
                l.getVehicle().getLicensePlate(),
                l.getLogTimestamp(),
                l.getOdometerKm(),
                l.getLitersFilled(),
                l.getPricePerLiter(),
                l.getTotalCost(),
                l.getGasStationName(),
                l.getDriverName(),
                l.getDistanceTraveledSinceLastFillKm(),
                l.getConsumptionRateKmPerLiter()
        );
    }
}

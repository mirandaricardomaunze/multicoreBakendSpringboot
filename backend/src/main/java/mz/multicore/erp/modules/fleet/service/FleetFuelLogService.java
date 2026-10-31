package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.events.FleetFuelLoggedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.fleet.dto.FuelLogDTO;
import mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest;
import mz.multicore.erp.modules.fleet.model.FuelLog;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class FleetFuelLogService {

    private final VehicleRepository vehicleRepository;
    private final FuelLogRepository fuelLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FleetFuelLogService(VehicleRepository vehicleRepository,
                               FuelLogRepository fuelLogRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.vehicleRepository = vehicleRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.eventPublisher = eventPublisher;
    }

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

        vehicle.setCurrentOdometerKm(request.odometerKm());
        if (consumptionRate.compareTo(BigDecimal.ZERO) > 0) {
            if (vehicle.getAverageFuelConsumptionKmPerLiter().compareTo(BigDecimal.ZERO) == 0) {
                vehicle.setAverageFuelConsumptionKmPerLiter(consumptionRate);
            } else {
                BigDecimal updatedAvg = vehicle.getAverageFuelConsumptionKmPerLiter().add(consumptionRate)
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                vehicle.setAverageFuelConsumptionKmPerLiter(updatedAvg);
            }
        }

        vehicleRepository.save(vehicle);
        FuelLog saved = fuelLogRepository.save(log);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new FleetFuelLoggedEvent(
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

    public List<FuelLogDTO> listFuelLogs(Long companyId, Long vehicleId) {
        vehicleRepository.findByIdAndCompanyId(vehicleId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Viatura não encontrada."));

        return fuelLogRepository.findByVehicleIdOrderByLogTimestampDesc(vehicleId)
                .stream()
                .map(this::toFuelDTO)
                .toList();
    }

    public FuelLogDTO toFuelDTO(FuelLog l) {
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

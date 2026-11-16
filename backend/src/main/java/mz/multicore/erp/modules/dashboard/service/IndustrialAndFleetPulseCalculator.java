package mz.multicore.erp.modules.dashboard.service;

import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import mz.multicore.erp.modules.fleet.model.FuelLog;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.service.VehicleLookup;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;
import mz.multicore.erp.modules.manufacturing.service.ProductionOrderLookup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class IndustrialAndFleetPulseCalculator {

    private final ProductionOrderLookup productionOrderLookup;
    private final VehicleLookup vehicleLookup;

    public IndustrialAndFleetPulseCalculator(ProductionOrderLookup productionOrderLookup,
                                             VehicleLookup vehicleLookup) {
        this.productionOrderLookup = productionOrderLookup;
        this.vehicleLookup = vehicleLookup;
    }

    public OperationalPulseDTO.ManufacturingPulse calculateManufacturingPulse(Long companyId) {
        List<ProductionOrder> orders = productionOrderLookup.listForCompany(companyId);
        long planned = 0;
        long inProgress = 0;
        long completed = 0;
        BigDecimal totalCip = BigDecimal.ZERO;

        for (ProductionOrder o : orders) {
            if (o.getStatus() == ProductionOrderStatus.PLANNED) {
                planned++;
            } else if (o.getStatus() == ProductionOrderStatus.IN_PROGRESS) {
                inProgress++;
            } else if (o.getStatus() == ProductionOrderStatus.COMPLETED) {
                completed++;
            }
            if (o.getTotalProductionCost() != null) {
                totalCip = totalCip.add(o.getTotalProductionCost());
            }
        }

        return new OperationalPulseDTO.ManufacturingPulse(planned, inProgress, completed, totalCip);
    }

    public OperationalPulseDTO.FleetPulse calculateFleetPulse(Long companyId) {
        List<Vehicle> vehicles = vehicleLookup.listForCompany(companyId);
        long active = vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.OPERATIONAL).count();
        long alerts = vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.MAINTENANCE || v.getStatus() == VehicleStatus.OUT_OF_SERVICE).count();

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal totalLiters = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalEfficiency = BigDecimal.ZERO;
        int efficiencyCount = 0;

        for (Vehicle v : vehicles) {
            List<FuelLog> logs = vehicleLookup.listFuelLogsForVehicle(v.getId());
            for (FuelLog log : logs) {
                if (log.getLogTimestamp() != null && log.getLogTimestamp().isAfter(thirtyDaysAgo)) {
                    if (log.getLitersFilled() != null) totalLiters = totalLiters.add(log.getLitersFilled());
                    if (log.getTotalCost() != null) totalCost = totalCost.add(log.getTotalCost());
                    if (log.getConsumptionRateKmPerLiter() != null && log.getConsumptionRateKmPerLiter().compareTo(BigDecimal.ZERO) > 0) {
                        totalEfficiency = totalEfficiency.add(log.getConsumptionRateKmPerLiter());
                        efficiencyCount++;
                    }
                }
            }
        }

        BigDecimal avgEfficiency = efficiencyCount > 0
                ? totalEfficiency.divide(BigDecimal.valueOf(efficiencyCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new OperationalPulseDTO.FleetPulse(
                active,
                vehicles.size(),
                totalLiters,
                totalCost,
                avgEfficiency,
                alerts
        );
    }
}

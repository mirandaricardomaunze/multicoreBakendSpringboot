package mz.multicore.erp.modules.dashboard.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import mz.multicore.erp.modules.fleet.model.FuelLog;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationalPulseService {

    private final ProductionOrderRepository productionOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelLogRepository fuelLogRepository;
    private final RecurringContractRepository recurringContractRepository;
    private final AssetRepository assetRepository;

    public OperationalPulseService(
            ProductionOrderRepository productionOrderRepository,
            VehicleRepository vehicleRepository,
            FuelLogRepository fuelLogRepository,
            RecurringContractRepository recurringContractRepository,
            AssetRepository assetRepository
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.recurringContractRepository = recurringContractRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public OperationalPulseDTO getOperationalPulse() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            return OperationalPulseDTO.empty();
        }

        OperationalPulseDTO.ManufacturingPulse manufacturing = calculateManufacturingPulse(companyId);
        OperationalPulseDTO.FleetPulse fleet = calculateFleetPulse(companyId);
        OperationalPulseDTO.RecurringPulse recurring = calculateRecurringPulse(companyId);
        OperationalPulseDTO.AssetPulse assets = calculateAssetPulse(companyId);

        return new OperationalPulseDTO(manufacturing, fleet, recurring, assets);
    }

    private OperationalPulseDTO.ManufacturingPulse calculateManufacturingPulse(Long companyId) {
        List<ProductionOrder> orders = productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
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

    private OperationalPulseDTO.FleetPulse calculateFleetPulse(Long companyId) {
        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(companyId);
        long active = vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.OPERATIONAL).count();
        long alerts = vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.MAINTENANCE || v.getStatus() == VehicleStatus.OUT_OF_SERVICE).count();

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal totalLiters = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalEfficiency = BigDecimal.ZERO;
        int efficiencyCount = 0;

        for (Vehicle v : vehicles) {
            List<FuelLog> logs = fuelLogRepository.findByVehicleIdOrderByLogTimestampDesc(v.getId());
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

    private OperationalPulseDTO.RecurringPulse calculateRecurringPulse(Long companyId) {
        List<RecurringContract> contracts = recurringContractRepository.findByCompanyIdWithClient(companyId);
        long activeCount = 0;
        BigDecimal mrr = BigDecimal.ZERO;
        long dueCount = 0;
        LocalDate today = LocalDate.now();

        for (RecurringContract c : contracts) {
            if (c.getStatus() == ContractStatus.ACTIVE) {
                activeCount++;
                BigDecimal amount = c.getFixedAmount() != null ? c.getFixedAmount() : BigDecimal.ZERO;
                if (c.getFrequency() == BillingFrequency.MONTHLY) {
                    mrr = mrr.add(amount);
                } else if (c.getFrequency() == BillingFrequency.QUARTERLY) {
                    mrr = mrr.add(amount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP));
                } else if (c.getFrequency() == BillingFrequency.ANNUAL) {
                    mrr = mrr.add(amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));
                } else if (c.getFrequency() == BillingFrequency.BIANNUAL) {
                    mrr = mrr.add(amount.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP));
                }

                if (c.getNextBillingDate() != null && !c.getNextBillingDate().isAfter(today)) {
                    dueCount++;
                }
            }
        }

        return new OperationalPulseDTO.RecurringPulse(activeCount, mrr, dueCount);
    }

    private OperationalPulseDTO.AssetPulse calculateAssetPulse(Long companyId) {
        List<Asset> assets = assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

        for (Asset a : assets) {
            if (a.getStatus() != AssetStatus.WRITTEN_OFF) {
                if (a.getAcquisitionCost() != null) {
                    totalCost = totalCost.add(a.getAcquisitionCost());
                }
                if (a.getAccumulatedDepreciation() != null) {
                    accumulatedDepreciation = accumulatedDepreciation.add(a.getAccumulatedDepreciation());
                }
            }
        }

        BigDecimal netBookValue = totalCost.subtract(accumulatedDepreciation).max(BigDecimal.ZERO);

        return new OperationalPulseDTO.AssetPulse(
                assets.size(),
                totalCost,
                accumulatedDepreciation,
                netBookValue
        );
    }
}

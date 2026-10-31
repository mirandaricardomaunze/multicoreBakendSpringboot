package mz.multicore.erp.modules.dashboard.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalPulseService {

    private final IndustrialAndFleetPulseCalculator industrialAndFleetCalculator;
    private final FinancialContractsAndAssetsPulseCalculator financialAndAssetsCalculator;

    @Autowired
    public OperationalPulseService(
            IndustrialAndFleetPulseCalculator industrialAndFleetCalculator,
            FinancialContractsAndAssetsPulseCalculator financialAndAssetsCalculator
    ) {
        this.industrialAndFleetCalculator = industrialAndFleetCalculator;
        this.financialAndAssetsCalculator = financialAndAssetsCalculator;
    }

    public OperationalPulseService(
            ProductionOrderRepository productionOrderRepository,
            VehicleRepository vehicleRepository,
            FuelLogRepository fuelLogRepository,
            RecurringContractRepository recurringContractRepository,
            AssetRepository assetRepository
    ) {
        this(new IndustrialAndFleetPulseCalculator(productionOrderRepository, vehicleRepository, fuelLogRepository),
             new FinancialContractsAndAssetsPulseCalculator(recurringContractRepository, assetRepository));
    }

    @Transactional(readOnly = true)
    public OperationalPulseDTO getOperationalPulse() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            return OperationalPulseDTO.empty();
        }

        return new OperationalPulseDTO(
                industrialAndFleetCalculator.calculateManufacturingPulse(companyId),
                industrialAndFleetCalculator.calculateFleetPulse(companyId),
                financialAndAssetsCalculator.calculateRecurringPulse(companyId),
                financialAndAssetsCalculator.calculateAssetPulse(companyId)
        );
    }
}

package mz.multicore.erp.modules.dashboard.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.assets.service.AssetLookup;
import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import mz.multicore.erp.modules.fleet.service.VehicleLookup;
import mz.multicore.erp.modules.manufacturing.service.ProductionOrderLookup;
import mz.multicore.erp.modules.recurring.service.RecurringContractLookup;
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
            ProductionOrderLookup productionOrderLookup,
            VehicleLookup vehicleLookup,
            RecurringContractLookup recurringContractLookup,
            AssetLookup assetLookup
    ) {
        this(new IndustrialAndFleetPulseCalculator(productionOrderLookup, vehicleLookup),
             new FinancialContractsAndAssetsPulseCalculator(recurringContractLookup, assetLookup));
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

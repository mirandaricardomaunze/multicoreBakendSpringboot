package mz.multicore.erp.modules.dashboard.service;

import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
public class FinancialContractsAndAssetsPulseCalculator {

    private final RecurringContractRepository recurringContractRepository;
    private final AssetRepository assetRepository;

    public FinancialContractsAndAssetsPulseCalculator(RecurringContractRepository recurringContractRepository,
                                                      AssetRepository assetRepository) {
        this.recurringContractRepository = recurringContractRepository;
        this.assetRepository = assetRepository;
    }

    public OperationalPulseDTO.RecurringPulse calculateRecurringPulse(Long companyId) {
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

    public OperationalPulseDTO.AssetPulse calculateAssetPulse(Long companyId) {
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
        return new OperationalPulseDTO.AssetPulse(assets.size(), totalCost, accumulatedDepreciation, netBookValue);
    }
}

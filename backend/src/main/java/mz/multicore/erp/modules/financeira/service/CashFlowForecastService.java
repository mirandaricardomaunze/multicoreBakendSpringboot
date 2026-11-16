package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Orquestrador de projecção previsional de fluxo de caixa e alertas de liquidez. */
@Service
public class CashFlowForecastService {

    private final CashFlowDataCollector dataCollector;
    private final CashFlowProjectionCalculator projectionCalculator;

    @Autowired
    public CashFlowForecastService(CashFlowDataCollector dataCollector,
                                   CashFlowProjectionCalculator projectionCalculator) {
        this.dataCollector = dataCollector;
        this.projectionCalculator = projectionCalculator;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public CashFlowForecastService(TreasuryAccountRepository accountRepository,
                                   InvoiceLookup invoiceLookup,
                                   SupplierLookup supplierLookup) {
        this(new CashFlowDataCollector(accountRepository, invoiceLookup, supplierLookup),
                new CashFlowProjectionCalculator());
    }

    @Transactional(readOnly = true)
    public CashFlowForecastDTO generateForecast() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return generateForecastForCompany(companyId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public CashFlowForecastDTO generateForecastForCompany(Long companyId, LocalDate referenceDate) {
        PermissionGuard.requireManagerOrAdmin("consultar projeção previsional de fluxo de caixa");
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now();
        return projectionCalculator.calculate(today, dataCollector.collect(companyId, today));
    }
}

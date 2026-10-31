package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.service.CashFlowForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fachada para emissão PDF A4 do Mapa Previsional de Fluxo de Caixa. */
@Service
public class CashFlowForecastPrintService {

    private final CashFlowForecastService forecastService;
    private final CompanyService companyService;
    private final CashFlowForecastDocumentRenderer documentRenderer;

    @Autowired
    public CashFlowForecastPrintService(CashFlowForecastService forecastService,
                                        CompanyService companyService,
                                        CashFlowForecastDocumentRenderer documentRenderer) {
        this.forecastService = forecastService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public CashFlowForecastPrintService(CashFlowForecastService forecastService,
                                        CompanyService companyService) {
        this(forecastService, companyService, new CashFlowForecastDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }
        CashFlowForecastDTO forecast = forecastService.generateForecastForCompany(companyId, null);
        return documentRenderer.render(company, forecast);
    }
}

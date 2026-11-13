package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fachada tenant-scoped para emissão de cotações. */
@Service
public class QuotationPrintService {
    private final OrderLookup orderLookup;
    private final QuotationA4Renderer a4Renderer;
    private final QuotationThermalRenderer thermalRenderer;

    @Autowired
    public QuotationPrintService(OrderLookup orderLookup, QuotationA4Renderer a4Renderer,
                                 QuotationThermalRenderer thermalRenderer) {
        this.orderLookup = orderLookup;
        this.a4Renderer = a4Renderer;
        this.thermalRenderer = thermalRenderer;
    }

    /** Preserva a construção usada antes da decomposição. */
    public QuotationPrintService(OrderLookup orderLookup, LineRowMapper lineRowMapper,
                                 DocumentConfigService documentConfigService) {
        this(orderLookup, new QuotationA4Renderer(lineRowMapper, documentConfigService),
                new QuotationThermalRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long quotationId) {
        return a4Renderer.render(loadOwned(quotationId));
    }

    @Transactional(readOnly = true)
    public byte[] renderThermal(Long quotationId) {
        return thermalRenderer.render(loadOwned(quotationId));
    }

    private Quotation loadOwned(Long quotationId) {
        Quotation quotation = orderLookup
                .findQuotationWithLines(quotationId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cotação não encontrada."));
        CurrentUserContext.requireCompany(quotation.getCompany().getId());
        return quotation;
    }
}

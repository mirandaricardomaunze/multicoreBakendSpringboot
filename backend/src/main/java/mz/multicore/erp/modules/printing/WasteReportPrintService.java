package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.WasteSummaryDTO;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.service.StockWasteLookup;
import mz.multicore.erp.modules.inventory.service.StockWasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Fachada para emissão PDF A4 do Relatório de Quebras e Perdas de Stock. */
@Service
public class WasteReportPrintService {

    private final StockWasteLookup wasteLookup;
    private final StockWasteService wasteService;
    private final CompanyService companyService;
    private final WasteReportDocumentRenderer documentRenderer;

    @Autowired
    public WasteReportPrintService(StockWasteLookup wasteLookup,
                                   StockWasteService wasteService,
                                   CompanyService companyService,
                                   WasteReportDocumentRenderer documentRenderer) {
        this.wasteLookup = wasteLookup;
        this.wasteService = wasteService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public WasteReportPrintService(StockWasteLookup wasteLookup,
                                   StockWasteService wasteService,
                                   CompanyService companyService) {
        this(wasteLookup, wasteService, companyService, new WasteReportDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, LocalDate start, LocalDate end) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        LocalDate effectiveStart = start != null ? start : LocalDate.now().minusDays(30);
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();
        LocalDateTime startDt = effectiveStart.atStartOfDay();
        LocalDateTime endDt = effectiveEnd.atTime(23, 59, 59);

        List<StockWaste> records = wasteLookup.listForCompanyInPeriod(companyId, startDt, endDt);
        WasteSummaryDTO summary = wasteService.getSummary(companyId, effectiveStart, effectiveEnd);
        return documentRenderer.render(company, effectiveStart, effectiveEnd, records, summary);
    }
}

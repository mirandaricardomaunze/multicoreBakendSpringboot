package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Livro de Inventário Valorizado de Fim de Exercício (Código Comercial de Moçambique & PGC-NIRF).
 * Exigido para encerramento de contas e apuramento do Custo das Mercadorias Vendidas e Matérias Consumidas (CMVMC).
 */
@Service
public class ValuedInventoryBookPrintService {

    private final StockLookup stockLookup;
    private final CompanyService companyService;
    private final ValuedInventoryBookDocumentRenderer documentRenderer;

    @Autowired
    public ValuedInventoryBookPrintService(StockLookup stockLookup,
                                           CompanyService companyService,
                                           ValuedInventoryBookDocumentRenderer documentRenderer) {
        this.stockLookup = stockLookup;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public ValuedInventoryBookPrintService(StockLookup stockLookup, CompanyService companyService) {
        this(stockLookup, companyService, new ValuedInventoryBookDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int fiscalYear) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        List<Stock> stocks = stockLookup.listForCompany(companyId).stream()
                .filter(s -> s.getQuantity() != null && s.getQuantity().signum() > 0)
                .sorted(Comparator.comparing(s -> s.getProduct() != null ? s.getProduct().getName() : ""))
                .toList();

        return documentRenderer.renderA4(company, fiscalYear, stocks);
    }
}

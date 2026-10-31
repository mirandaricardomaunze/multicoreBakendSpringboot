package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;
import mz.multicore.erp.modules.purchases.repository.PurchaseLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reposição automática e inteligente de stock:
 * Calcula a velocidade diária de vendas (últimos 30 dias), previsão de dias até à rutura,
 * status de criticidade (ESGOTADO, CRÍTICO, BAIXO), fornecedor habitual e custo estimado
 * arredondado a caixas inteiras.
 */
@Service
public class ReorderService {

    private final ProductRepository productRepository;
    private final ReorderDataCollector dataCollector;
    private final ReorderSuggestionCalculator suggestionCalculator;

    @Autowired
    public ReorderService(ProductRepository productRepository,
                          ReorderDataCollector dataCollector,
                          ReorderSuggestionCalculator suggestionCalculator) {
        this.productRepository = productRepository;
        this.dataCollector = dataCollector;
        this.suggestionCalculator = suggestionCalculator;
    }

    public ReorderService(
            StockRepository stockRepository,
            ProductRepository productRepository,
            InvoiceRepository invoiceRepository,
            PurchaseLineRepository purchaseLineRepository
    ) {
        this(productRepository,
             new ReorderDataCollector(stockRepository, invoiceRepository, purchaseLineRepository),
             new ReorderSuggestionCalculator());
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionDTO> suggestions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return suggestionCalculator.computeSuggestions(
                productRepository.findDistinctByCompaniesIdOrderByName(companyId),
                dataCollector.collectStockByProduct(companyId),
                dataCollector.collectSalesLast30Days(companyId),
                dataCollector.collectRecentPurchases(companyId)
        );
    }
}

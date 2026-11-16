package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.inventory.service.StockLookup;
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

    private final ProductLookup productLookup;
    private final ReorderDataCollector dataCollector;
    private final ReorderSuggestionCalculator suggestionCalculator;

    @Autowired
    public ReorderService(ProductLookup productLookup,
                          ReorderDataCollector dataCollector,
                          ReorderSuggestionCalculator suggestionCalculator) {
        this.productLookup = productLookup;
        this.dataCollector = dataCollector;
        this.suggestionCalculator = suggestionCalculator;
    }

    public ReorderService(
            StockLookup stockLookup,
            ProductLookup productLookup,
            InvoiceLookup invoiceLookup,
            PurchaseLineRepository purchaseLineRepository
    ) {
        this(productLookup,
             new ReorderDataCollector(stockLookup, invoiceLookup, purchaseLineRepository),
             new ReorderSuggestionCalculator());
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionDTO> suggestions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return suggestionCalculator.computeSuggestions(
                productLookup.listForCompany(companyId),
                dataCollector.collectStockByProduct(companyId),
                dataCollector.collectSalesLast30Days(companyId),
                dataCollector.collectRecentPurchases(companyId)
        );
    }
}

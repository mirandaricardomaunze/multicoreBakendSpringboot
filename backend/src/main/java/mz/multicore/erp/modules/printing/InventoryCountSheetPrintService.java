package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Folha de contagem de inventário — <b>contagem cega</b>: lista os artigos (referência/SKU, nome) com
 * uma coluna <b>Contagem</b> em branco para o funcionário escrever à mão.
 */
@Service
public class InventoryCountSheetPrintService {

    private final StockLookup stockLookup;
    private final WarehouseLookup warehouseLookup;
    private final CompanyService companyService;
    private final InventoryCountSheetDocumentRenderer documentRenderer;

    @Autowired
    public InventoryCountSheetPrintService(
            StockLookup stockLookup,
            WarehouseLookup warehouseLookup,
            CompanyService companyService,
            InventoryCountSheetDocumentRenderer documentRenderer
    ) {
        this.stockLookup = stockLookup;
        this.warehouseLookup = warehouseLookup;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public InventoryCountSheetPrintService(
            StockLookup stockLookup,
            WarehouseLookup warehouseLookup,
            CompanyService companyService
    ) {
        this(stockLookup, warehouseLookup, companyService, new InventoryCountSheetDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long warehouseId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        Warehouse warehouseFilter = null;
        List<Stock> stocks;
        if (warehouseId != null) {
            warehouseFilter = warehouseLookup.require(warehouseId);
            stocks = stockLookup.listForWarehouse(warehouseId);
        } else {
            stocks = stockLookup.listForCompany(companyId);
        }

        stocks.sort(Comparator
                .comparing((Stock s) -> s.getWarehouse().getName())
                .thenComparing(s -> s.getProduct().getName()));

        return documentRenderer.renderA4(company, warehouseFilter, stocks);
    }
}

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fachada para emissão PDF A4 do relatório de Inventário de Stock. */
@Service
public class InventoryReportPrintService {

    private final StockLookup stockLookup;
    private final WarehouseLookup warehouseLookup;
    private final CompanyService companyService;
    private final InventoryReportDocumentRenderer documentRenderer;

    @Autowired
    public InventoryReportPrintService(StockLookup stockLookup,
                                       WarehouseLookup warehouseLookup,
                                       CompanyService companyService,
                                       InventoryReportDocumentRenderer documentRenderer) {
        this.stockLookup = stockLookup;
        this.warehouseLookup = warehouseLookup;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public InventoryReportPrintService(StockLookup stockLookup,
                                       WarehouseLookup warehouseLookup,
                                       CompanyService companyService) {
        this(stockLookup, warehouseLookup, companyService, new InventoryReportDocumentRenderer());
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

        List<Stock> mutableStocks = new ArrayList<>(stocks != null ? stocks : List.of());
        mutableStocks.sort(Comparator
                .comparing((Stock s) -> s.getWarehouse() != null && s.getWarehouse().getName() != null
                        ? s.getWarehouse().getName() : "", String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.getProduct() != null && s.getProduct().getName() != null
                        ? s.getProduct().getName() : "", String.CASE_INSENSITIVE_ORDER));

        return documentRenderer.render(company, warehouseFilter, mutableStocks);
    }
}

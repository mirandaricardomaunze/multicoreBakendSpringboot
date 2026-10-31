package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
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

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyService companyService;
    private final InventoryCountSheetDocumentRenderer documentRenderer;

    @Autowired
    public InventoryCountSheetPrintService(
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            CompanyService companyService,
            InventoryCountSheetDocumentRenderer documentRenderer
    ) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public InventoryCountSheetPrintService(
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            CompanyService companyService
    ) {
        this(stockRepository, warehouseRepository, companyService, new InventoryCountSheetDocumentRenderer());
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
            warehouseFilter = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
            stocks = stockRepository.findByWarehouseId(warehouseId);
        } else {
            stocks = stockRepository.findByWarehouseCompanyId(companyId);
        }

        stocks.sort(Comparator
                .comparing((Stock s) -> s.getWarehouse().getName())
                .thenComparing(s -> s.getProduct().getName()));

        return documentRenderer.renderA4(company, warehouseFilter, stocks);
    }
}

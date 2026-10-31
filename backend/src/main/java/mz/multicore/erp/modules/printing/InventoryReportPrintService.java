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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fachada para emissão PDF A4 do relatório de Inventário de Stock. */
@Service
public class InventoryReportPrintService {

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyService companyService;
    private final InventoryReportDocumentRenderer documentRenderer;

    @Autowired
    public InventoryReportPrintService(StockRepository stockRepository,
                                       WarehouseRepository warehouseRepository,
                                       CompanyService companyService,
                                       InventoryReportDocumentRenderer documentRenderer) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public InventoryReportPrintService(StockRepository stockRepository,
                                       WarehouseRepository warehouseRepository,
                                       CompanyService companyService) {
        this(stockRepository, warehouseRepository, companyService, new InventoryReportDocumentRenderer());
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

        List<Stock> mutableStocks = new ArrayList<>(stocks != null ? stocks : List.of());
        mutableStocks.sort(Comparator
                .comparing((Stock s) -> s.getWarehouse() != null && s.getWarehouse().getName() != null
                        ? s.getWarehouse().getName() : "", String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.getProduct() != null && s.getProduct().getName() != null
                        ? s.getProduct().getName() : "", String.CASE_INSENSITIVE_ORDER));

        return documentRenderer.render(company, warehouseFilter, mutableStocks);
    }
}

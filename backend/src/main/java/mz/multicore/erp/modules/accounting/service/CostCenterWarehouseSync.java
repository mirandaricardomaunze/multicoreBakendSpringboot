package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.WarehouseSavedEvent;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.repository.CostCenterRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every warehouse is automatically a cost centre (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-02): saving a
 * warehouse creates its {@link CostCenterType#WAREHOUSE} centre (code = warehouse number) or makes the existing one
 * follow the warehouse's name and state. At start-up, warehouses still without a centre receive one — the same
 * backfill migration V150 does in PostgreSQL. A deactivated warehouse whose centre was the company default hands the
 * default back to {@code ADM} so the company always keeps one active default.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CostCenterWarehouseSync implements ApplicationRunner {

    static final String FALLBACK_CODE_PREFIX = "ARM-";
    private static final int CODE_LENGTH = 20;
    private static final int NAME_LENGTH = 120;
    private static final Logger log = LoggerFactory.getLogger(CostCenterWarehouseSync.class);

    private final CostCenterRepository repository;
    private final CostCenterService costCenterService;
    private final CompanyLookup companyLookup;
    private final WarehouseLookup warehouseLookup;

    public CostCenterWarehouseSync(CostCenterRepository repository, CostCenterService costCenterService,
                                   CompanyLookup companyLookup, WarehouseLookup warehouseLookup) {
        this.repository = repository;
        this.costCenterService = costCenterService;
        this.companyLookup = companyLookup;
        this.warehouseLookup = warehouseLookup;
    }

    @EventListener
    @Transactional
    public void onWarehouseSaved(WarehouseSavedEvent event) {
        if (event.warehouseId() == null || event.companyId() == null) return;
        CostCenter center = repository.findByWarehouseId(event.warehouseId())
                .orElseGet(() -> newWarehouseCenter(event));
        center.setName(limit(event.name(), NAME_LENGTH));
        center.setActive(event.active());
        boolean wasDefault = center.isDefaultCenter();
        if (!event.active()) {
            center.setDefaultCenter(false);
        }
        repository.save(center);
        if (wasDefault && !event.active()) {
            costCenterService.requireDefault(event.companyId());
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Company company : companyLookup.findAll()) {
            int created = 0;
            for (Warehouse warehouse : warehouseLookup.listForCompany(company.getId())) {
                if (repository.findByWarehouseId(warehouse.getId()).isPresent()) continue;
                onWarehouseSaved(new WarehouseSavedEvent(warehouse.getId(), company.getId(),
                        warehouse.getWarehouseNumber(), warehouse.getName(), warehouse.isActive()));
                created++;
            }
            if (created > 0) {
                log.info("Centros de custo de armazém criados na empresa {}: {}.", company.getId(), created);
            }
        }
    }

    private CostCenter newWarehouseCenter(WarehouseSavedEvent event) {
        CostCenter center = new CostCenter();
        center.setCompany(companyLookup.reference(event.companyId()));
        center.setCode(freeCode(event));
        center.setType(CostCenterType.WAREHOUSE);
        center.setWarehouseId(event.warehouseId());
        center.setCreatedBy("system");
        return center;
    }

    /** Warehouse number when free in the company; otherwise {@code ARM-<id>} (suffixed until free). */
    private String freeCode(WarehouseSavedEvent event) {
        String number = limit(event.warehouseNumber(), CODE_LENGTH);
        if (!number.isEmpty() && !repository.existsByCompanyIdAndCodeIgnoreCase(event.companyId(), number)) {
            return number;
        }
        String fallback = FALLBACK_CODE_PREFIX + event.warehouseId();
        String code = fallback;
        for (int n = 2; repository.existsByCompanyIdAndCodeIgnoreCase(event.companyId(), code); n++) {
            code = fallback + "-" + n;
        }
        return code;
    }

    private static String limit(String value, int length) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() > length ? trimmed.substring(0, length) : trimmed;
    }
}

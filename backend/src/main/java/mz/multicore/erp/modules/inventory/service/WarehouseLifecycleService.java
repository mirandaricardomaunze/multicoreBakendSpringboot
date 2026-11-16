package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.CreateWarehouseRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WarehouseLifecycleService {

    private final WarehouseRepository warehouseRepository;
    private final CompanyLookup companyLookup;
    private final CompanyService companyService;
    private final AuditLogService auditLogService;
    private final WarehouseQueryService queryService;

    public WarehouseLifecycleService(WarehouseRepository warehouseRepository,
                                     CompanyLookup companyLookup,
                                     CompanyService companyService,
                                     AuditLogService auditLogService,
                                     WarehouseQueryService queryService) {
        this.warehouseRepository = warehouseRepository;
        this.companyLookup = companyLookup;
        this.companyService = companyService;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
    }

    public Warehouse updateWarehouse(Long id, String name, String warehouseNumber, BigDecimal capacity,
                                     String location, WarehouseType type,
                                     boolean allowsSales, String manager, String phone) {
        Warehouse warehouse = queryService.loadWarehouseForActiveCompany(id);
        warehouse.setName(name);
        warehouse.setWarehouseNumber(warehouseNumber);
        warehouse.setCapacity(capacity);
        warehouse.setLocation(location);
        warehouse.setType(type == null ? WarehouseType.STORE : type);
        warehouse.setAllowsSales(allowsSales);
        warehouse.setManager(manager == null || manager.isBlank() ? null : manager.trim());
        warehouse.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        warehouse = warehouseRepository.save(warehouse);
        auditLogService.logCurrent("WAREHOUSE_UPDATE", "Armazém " + warehouse.getName() + " actualizado.");
        return warehouse;
    }

    public Warehouse setWarehouseActive(Long id, boolean active) {
        Warehouse warehouse = queryService.loadWarehouseForActiveCompany(id);
        warehouse.setActive(active);
        warehouse = warehouseRepository.save(warehouse);
        auditLogService.logCurrent("WAREHOUSE_STATUS",
                "Armazém " + warehouse.getName() + (active ? " activado." : " desactivado."));
        return warehouse;
    }

    public void setStockCountLocked(Long companyId, boolean locked) {
        Company company = companyLookup.require(companyId);
        company.setStockCountLocked(locked);
        companyService.saveCompany(company);
        auditLogService.logCurrent("STOCK_LOCK",
                locked ? "Stock trancado (quantidades visíveis só para administradores)."
                        : "Stock destrancado (quantidades visíveis para todos).");
    }

    public Warehouse createWarehouse(String name, String warehouseNumber, BigDecimal capacity, String location,
                                     WarehouseType type, boolean allowsSales,
                                     String manager, String phone, Company company) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setWarehouseNumber(warehouseNumber);
        warehouse.setCapacity(capacity);
        warehouse.setLocation(location);
        warehouse.setType(type == null ? WarehouseType.STORE : type);
        warehouse.setAllowsSales(allowsSales);
        warehouse.setActive(true);
        warehouse.setManager(manager == null || manager.isBlank() ? null : manager.trim());
        warehouse.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        warehouse.setCompany(company);
        warehouse.setCreatedBy("SYSTEM");
        return warehouseRepository.save(warehouse);
    }

    public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
        Company company = companyLookup.require(request.companyId());
        Warehouse warehouse = createWarehouse(
                request.name(),
                request.warehouseNumber(),
                request.capacity(),
                request.location(),
                request.type(),
                request.allowsSales(),
                request.manager(),
                request.phone(),
                company
        );
        return queryService.toDTO(warehouse);
    }
}

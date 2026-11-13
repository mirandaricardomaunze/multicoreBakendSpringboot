package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.CreateWarehouseRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gestor do ciclo de vida, permissões de venda e contagem cega de armazéns.
 */
@Service
public class WarehouseService {

    private final WarehouseLifecycleService lifecycleService;
    private final WarehouseQueryService queryService;

    @Autowired
    public WarehouseService(WarehouseLifecycleService lifecycleService,
                            WarehouseQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    public WarehouseService(WarehouseRepository warehouseRepository, CompanyLookup companyLookup,
                            CompanyService companyService, AuditLogService auditLogService) {
        WarehouseQueryService query = new WarehouseQueryService(warehouseRepository, companyLookup);
        this.queryService = query;
        this.lifecycleService = new WarehouseLifecycleService(
                warehouseRepository, companyLookup, companyService, auditLogService, query);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.getWarehousesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getSalesWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.getSalesWarehousesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.getAllWarehousesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> resolveSalesWarehouses(Long companyId, Long warehouseId) {
        return queryService.resolveSalesWarehouses(companyId, warehouseId);
    }

    public Warehouse loadWarehouseForActiveCompany(Long id) {
        return queryService.loadWarehouseForActiveCompany(id);
    }

    public Warehouse requireWarehouseCompany(Long warehouseId) {
        return queryService.requireWarehouseCompany(warehouseId);
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, String name, String warehouseNumber, BigDecimal capacity,
                                     String location, WarehouseType type,
                                     boolean allowsSales, String manager, String phone) {
        return lifecycleService.updateWarehouse(id, name, warehouseNumber, capacity, location, type, allowsSales, manager, phone);
    }

    @Transactional
    public Warehouse setWarehouseActive(Long id, boolean active) {
        PermissionGuard.requireManagerOrAdmin(active ? "activar armazém" : "desactivar armazém");
        return lifecycleService.setWarehouseActive(id, active);
    }

    @Transactional(readOnly = true)
    public boolean isStockCountLocked(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.isStockCountLocked(companyId);
    }

    @Transactional
    public void setStockCountLocked(Long companyId, boolean locked) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireAdmin(locked ? "trancar o stock" : "destrancar o stock");
        lifecycleService.setStockCountLocked(companyId, locked);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String location, Company company) {
        return createWarehouse(name, null, null, location, company);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String warehouseNumber, BigDecimal capacity, String location, Company company) {
        return createWarehouse(name, warehouseNumber, capacity, location,
                WarehouseType.STORE, true, null, null, company);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String warehouseNumber, BigDecimal capacity, String location,
                                     WarehouseType type, boolean allowsSales,
                                     String manager, String phone, Company company) {
        return lifecycleService.createWarehouse(name, warehouseNumber, capacity, location, type, allowsSales, manager, phone, company);
    }

    @Transactional
    public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
        return lifecycleService.createWarehouse(request);
    }

    @Transactional(readOnly = true)
    public List<WarehouseDTO> findWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.findWarehousesByCompany(companyId);
    }

    public WarehouseDTO toDTO(Warehouse w) {
        return queryService.toDTO(w);
    }
}

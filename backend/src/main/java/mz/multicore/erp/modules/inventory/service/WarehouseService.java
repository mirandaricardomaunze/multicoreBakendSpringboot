package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.CreateWarehouseRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gestor do ciclo de vida, permissões de venda e contagem cega de armazéns.
 */
@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public WarehouseService(
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            AuditLogService auditLogService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId).stream()
                .filter(Warehouse::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getSalesWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId).stream()
                .filter(Warehouse::isActive)
                .filter(Warehouse::isAllowsSales)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> resolveSalesWarehouses(Long companyId, Long warehouseId) {
        if (warehouseId == null) {
            return getSalesWarehousesByCompany(companyId);
        }
        Warehouse w = loadWarehouseForActiveCompany(warehouseId);
        return (w != null && w.isActive() && w.isAllowsSales()) ? List.of(w) : List.of();
    }

    public Warehouse loadWarehouseForActiveCompany(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        CurrentUserContext.requireCompany(warehouse.getCompany() != null ? warehouse.getCompany().getId() : null);
        return warehouse;
    }

    public Warehouse requireWarehouseCompany(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        CurrentUserContext.requireCompany(warehouse.getCompany().getId());
        return warehouse;
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, String name, String warehouseNumber, BigDecimal capacity,
                                     String location, WarehouseType type,
                                     boolean allowsSales, String manager, String phone) {
        Warehouse warehouse = loadWarehouseForActiveCompany(id);
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

    @Transactional
    public Warehouse setWarehouseActive(Long id, boolean active) {
        PermissionGuard.requireManagerOrAdmin(active ? "activar armazém" : "desactivar armazém");
        Warehouse warehouse = loadWarehouseForActiveCompany(id);
        warehouse.setActive(active);
        warehouse = warehouseRepository.save(warehouse);
        auditLogService.logCurrent("WAREHOUSE_STATUS",
                "Armazém " + warehouse.getName() + (active ? " activado." : " desactivado."));
        return warehouse;
    }

    @Transactional(readOnly = true)
    public boolean isStockCountLocked(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return companyRepository.findById(companyId)
                .map(Company::isStockCountLocked)
                .orElse(false);
    }

    @Transactional
    public void setStockCountLocked(Long companyId, boolean locked) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireAdmin(locked ? "trancar o stock" : "destrancar o stock");
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        company.setStockCountLocked(locked);
        companyRepository.save(company);
        auditLogService.logCurrent("STOCK_LOCK",
                locked ? "Stock trancado (quantidades visíveis só para administradores)."
                        : "Stock destrancado (quantidades visíveis para todos).");
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

    @Transactional
    public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
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
        return toDTO(warehouse);
    }

    @Transactional(readOnly = true)
    public List<WarehouseDTO> findWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    public WarehouseDTO toDTO(Warehouse w) {
        return new WarehouseDTO(
                w.getId(),
                w.getName(),
                w.getLocation(),
                w.getWarehouseNumber(),
                w.getCapacity(),
                w.getCompany() != null ? w.getCompany().getId() : null,
                w.getType(),
                w.isAllowsSales(),
                w.getManager(),
                w.getPhone(),
                w.isActive()
        );
    }
}

package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WarehouseQueryService {
    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;

    public WarehouseQueryService(WarehouseRepository warehouseRepository,
                                 CompanyRepository companyRepository) {
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
    }

    public List<Warehouse> getWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId).stream()
                .filter(Warehouse::isActive)
                .toList();
    }

    public List<Warehouse> getSalesWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId).stream()
                .filter(Warehouse::isActive)
                .filter(Warehouse::isAllowsSales)
                .toList();
    }

    public List<Warehouse> getAllWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId);
    }

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

    public boolean isStockCountLocked(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return companyRepository.findById(companyId)
                .map(Company::isStockCountLocked)
                .orElse(false);
    }

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

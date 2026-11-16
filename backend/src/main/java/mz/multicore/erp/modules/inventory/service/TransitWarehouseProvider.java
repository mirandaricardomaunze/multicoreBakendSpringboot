package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Component;

/**
 * Company warehouses for goods that left the shop but still belong to the company: bread at resellers ("Em
 * consignação"), crates and gas bottles at clients ("Vasilhame em clientes"). In transit and not for sale at the
 * POS. Found by name and created on first use.
 */
@Component
public class TransitWarehouseProvider {

    private final WarehouseRepository warehouseRepository;

    public TransitWarehouseProvider(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public Warehouse named(Company company, String name, String location) {
        return warehouseRepository.findByCompanyId(company.getId()).stream()
                .filter(w -> w.getType() == WarehouseType.TRANSIT && name.equals(w.getName()))
                .findFirst().orElseGet(() -> {
                    Warehouse w = new Warehouse();
                    w.setName(name);
                    w.setLocation(location);
                    w.setType(WarehouseType.TRANSIT);
                    w.setAllowsSales(false);
                    w.setActive(true);
                    w.setCompany(company);
                    return warehouseRepository.save(w);
                });
    }
}

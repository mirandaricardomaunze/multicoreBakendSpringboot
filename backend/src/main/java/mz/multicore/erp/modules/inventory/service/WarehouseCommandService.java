package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escrita de armazéns pedida por outros módulos (seeders), sem expor o {@link WarehouseRepository}
 * (docs/MODULE_DECOUPLING_SPEC.md §2). Leituras passam por {@link WarehouseLookup}.
 */
@Service
@Transactional
public class WarehouseCommandService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseCommandService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public Warehouse saveWarehouse(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }
}

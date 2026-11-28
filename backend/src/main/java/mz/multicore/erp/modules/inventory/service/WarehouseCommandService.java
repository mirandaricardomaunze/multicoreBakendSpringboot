package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.WarehouseSavedEvent;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Única escrita de armazéns: a do próprio inventário e a pedida por outros módulos (seeders), sem expor o
 * {@link WarehouseRepository} (docs/MODULE_DECOUPLING_SPEC.md §2). Leituras passam por {@link WarehouseLookup}.
 * Cada gravação anuncia {@link WarehouseSavedEvent} para o centro de custo acompanhar o armazém
 * (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-02).
 */
@Service
@Transactional
public class WarehouseCommandService {

    private final WarehouseRepository warehouseRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WarehouseCommandService(WarehouseRepository warehouseRepository, ApplicationEventPublisher eventPublisher) {
        this.warehouseRepository = warehouseRepository;
        this.eventPublisher = eventPublisher;
    }

    public Warehouse saveWarehouse(Warehouse warehouse) {
        Warehouse saved = warehouseRepository.save(warehouse);
        Long companyId = saved.getCompany() == null ? null : saved.getCompany().getId();
        eventPublisher.publishEvent(new WarehouseSavedEvent(saved.getId(), companyId, saved.getWarehouseNumber(),
                saved.getName(), saved.isActive()));
        return saved;
    }
}

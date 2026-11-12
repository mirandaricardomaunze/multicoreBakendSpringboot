package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de armazéns para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * É o único ponto pelo qual um módulo alheio lê {@link Warehouse}; escritas passam por
 * {@link WarehouseCommandService}. As entidades devolvidas servem só para relações JPA dentro do backend
 * e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class WarehouseLookup {

    static final String NOT_FOUND = "Armazém não encontrado.";
    static final String NOT_FOUND_IN_COMPANY = "Armazém não encontrado nesta empresa.";

    private final WarehouseRepository warehouseRepository;

    public WarehouseLookup(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    /** Armazém pelo id; {@code BusinessRuleException("Armazém não encontrado.")} se não existir. */
    public Warehouse require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Warehouse> find(Long id) {
        return id == null ? Optional.empty() : warehouseRepository.findById(id);
    }

    /** Armazém da empresa; {@code BusinessRuleException("Armazém não encontrado nesta empresa.")} caso contrário. */
    public Warehouse requireForCompany(Long id, Long companyId) {
        return findForCompany(id, companyId).orElseThrow(() -> new BusinessRuleException(NOT_FOUND_IN_COMPANY));
    }

    public Optional<Warehouse> findForCompany(Long id, Long companyId) {
        return find(id).filter(w -> w.getCompany() != null && w.getCompany().getId() != null
                && w.getCompany().getId().equals(companyId));
    }

    /** Armazéns da empresa (activos e inactivos), pela ordem do repositório. */
    public List<Warehouse> listForCompany(Long companyId) {
        return warehouseRepository.findByCompanyId(companyId);
    }
}

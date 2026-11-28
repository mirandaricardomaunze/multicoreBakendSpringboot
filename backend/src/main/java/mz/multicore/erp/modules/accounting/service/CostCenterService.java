package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.dto.SaveCostCenterRequest;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.repository.CostCenterRepository;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cost centre register of the active company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-01): managers create,
 * rename, deactivate and delete manual centres; warehouse centres follow their warehouse and are not edited by hand;
 * a centre with journal lines is never deleted; postings only accept an active centre of the same company.
 * Each company has a default centre, born as {@code ADM — Administração} and changeable to another active centre
 * (CC-03); the default centre is never deactivated or deleted.
 */
@Service
public class CostCenterService {

    static final String DEFAULT_CODE = "ADM";
    static final String DEFAULT_NAME = "Administração";

    private final CostCenterRepository repository;
    private final CompanyService companyService;

    public CostCenterService(CostCenterRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    /** Centres of the active company; listing is a "first use", so the list always shows the default centre (CC-09). */
    @Transactional
    public List<CostCenterDTO> list() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        requireDefault(companyId);
        return repository.findByCompanyIdOrderByCode(companyId).stream()
                .map(CostCenterService::toDTO).toList();
    }

    @Transactional
    public CostCenterDTO create(SaveCostCenterRequest request) {
        PermissionGuard.requireManagerOrAdmin("criar centro de custo");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String code = requireText(request.code(), "O código do centro de custo é obrigatório.");
        String name = requireText(request.name(), "O nome do centro de custo é obrigatório.");
        requireFreeCode(companyId, code);

        CostCenter center = new CostCenter();
        center.setCompany(companyService.getCurrentCompanyReference(companyId));
        center.setCode(code);
        center.setName(name);
        center.setType(CostCenterType.MANUAL);
        center.setActive(request.active() == null || request.active());
        center.setCreatedBy(CurrentUserContext.getUsername());
        return toDTO(repository.save(center));
    }

    @Transactional
    public CostCenterDTO update(Long id, SaveCostCenterRequest request) {
        PermissionGuard.requireManagerOrAdmin("alterar centro de custo");
        CostCenter center = requireManualOfActiveCompany(id);
        String code = requireText(request.code(), "O código do centro de custo é obrigatório.");
        String name = requireText(request.name(), "O nome do centro de custo é obrigatório.");
        if (!code.equalsIgnoreCase(center.getCode())) {
            requireFreeCode(center.getCompany().getId(), code);
        }
        if (Boolean.FALSE.equals(request.active())) {
            requireNotDefault(center, "desactivado");
        }
        center.setCode(code);
        center.setName(name);
        if (request.active() != null) {
            center.setActive(request.active());
        }
        return toDTO(repository.save(center));
    }

    @Transactional
    public void delete(Long id) {
        PermissionGuard.requireManagerOrAdmin("apagar centro de custo");
        CostCenter center = requireManualOfActiveCompany(id);
        requireNotDefault(center, "apagado");
        if (repository.hasJournalLines(center.getId())) {
            throw new BusinessRuleException("O centro de custo " + center.getCode()
                    + " já tem lançamentos e não pode ser apagado — desactive-o.");
        }
        repository.delete(center);
    }

    /** Centre a new journal line may use: exists, belongs to {@code companyId} and is active. */
    @Transactional(readOnly = true)
    public CostCenter requirePostable(Long companyId, Long id) {
        CostCenter center = requireOfCompany(companyId, id);
        if (!center.isActive()) {
            throw new BusinessRuleException("O centro de custo " + center.getCode()
                    + " está inactivo e não aceita lançamentos.");
        }
        return center;
    }

    /**
     * Default centre of {@code companyId} (CC-03): used by postings without a warehouse. A company that still has none
     * gets {@code ADM — Administração} (an existing {@code ADM} centre is promoted instead of duplicated).
     */
    @Transactional
    public CostCenter requireDefault(Long companyId) {
        return repository.findFirstByCompanyIdAndDefaultCenterTrue(companyId)
                .orElseGet(() -> {
                    CostCenter adm = repository.findByCompanyIdAndCodeIgnoreCase(companyId, DEFAULT_CODE)
                            .orElseGet(() -> newAdministrationCenter(companyId));
                    adm.setDefaultCenter(true);
                    return repository.save(adm);
                });
    }

    /**
     * Centre of an automatic posting (CC-05, SPEC §3.2): the active centre of the source document's warehouse; the
     * default centre when the event has no warehouse or the warehouse has no active centre of {@code companyId}.
     */
    @Transactional
    public CostCenter postingCenterOf(Long companyId, Long warehouseId) {
        if (warehouseId == null) {
            return requireDefault(companyId);
        }
        return repository.findByWarehouseId(warehouseId)
                .filter(center -> center.belongsTo(companyId) && center.isActive())
                .orElseGet(() -> requireDefault(companyId));
    }

    /** Makes another active centre of the active company its default centre (CC-03). */
    @Transactional
    public CostCenterDTO setDefault(Long id) {
        PermissionGuard.requireManagerOrAdmin("alterar o centro de custo por omissão");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        CostCenter center = requirePostable(companyId, id);
        repository.findFirstByCompanyIdAndDefaultCenterTrue(companyId)
                .filter(current -> !current.getId().equals(center.getId()))
                .ifPresent(current -> {
                    current.setDefaultCenter(false);
                    repository.save(current);
                });
        center.setDefaultCenter(true);
        return toDTO(repository.save(center));
    }

    private CostCenter newAdministrationCenter(Long companyId) {
        CostCenter adm = new CostCenter();
        adm.setCompany(companyService.getCurrentCompanyReference(companyId));
        adm.setCode(DEFAULT_CODE);
        adm.setName(DEFAULT_NAME);
        adm.setType(CostCenterType.MANUAL);
        adm.setActive(true);
        adm.setCreatedBy(CurrentUserContext.getUsername());
        return adm;
    }

    private static void requireNotDefault(CostCenter center, String action) {
        if (center.isDefaultCenter()) {
            throw new BusinessRuleException("O centro de custo " + center.getCode()
                    + " é o centro por omissão e não pode ser " + action
                    + " — escolha antes outro centro por omissão.");
        }
    }

    private CostCenter requireManualOfActiveCompany(Long id) {
        CostCenter center = requireOfCompany(CurrentUserContext.getCurrentCompanyId(), id);
        if (center.getType() == CostCenterType.WAREHOUSE) {
            throw new BusinessRuleException("O centro de custo " + center.getCode()
                    + " é de um armazém e acompanha o armazém — altere o armazém.");
        }
        return center;
    }

    private CostCenter requireOfCompany(Long companyId, Long id) {
        return repository.findById(id)
                .filter(center -> center.belongsTo(companyId))
                .orElseThrow(() -> new BusinessRuleException("Centro de custo não encontrado nesta empresa."));
    }

    private void requireFreeCode(Long companyId, String code) {
        if (repository.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new BusinessRuleException("Já existe o centro de custo " + code + " nesta empresa.");
        }
    }

    private static String requireText(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessRuleException(message);
        }
        return trimmed;
    }

    static CostCenterDTO toDTO(CostCenter center) {
        return new CostCenterDTO(center.getId(), center.getCode(), center.getName(), center.getType(),
                center.getWarehouseId(), center.isActive(), center.isDefaultCenter());
    }
}

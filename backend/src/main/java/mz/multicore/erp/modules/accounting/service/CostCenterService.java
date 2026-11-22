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
 */
@Service
public class CostCenterService {

    private final CostCenterRepository repository;
    private final CompanyService companyService;

    public CostCenterService(CostCenterRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public List<CostCenterDTO> list() {
        return repository.findByCompanyIdOrderByCode(CurrentUserContext.getCurrentCompanyId()).stream()
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
                center.getWarehouseId(), center.isActive());
    }
}

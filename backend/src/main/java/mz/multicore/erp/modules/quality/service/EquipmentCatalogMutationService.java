package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.quality.dto.EquipmentCategory;
import mz.multicore.erp.modules.quality.dto.EquipmentCreateRequest;
import mz.multicore.erp.modules.quality.dto.EquipmentDTO;
import mz.multicore.erp.modules.quality.dto.PeriodUnit;
import mz.multicore.erp.modules.quality.model.Equipment;
import mz.multicore.erp.modules.quality.repository.EquipmentRepository;
import org.springframework.stereotype.Component;

@Component
public class EquipmentCatalogMutationService {

    private final EquipmentRepository equipmentRepository;
    private final CompanyService companyService;

    public EquipmentCatalogMutationService(EquipmentRepository equipmentRepository, CompanyService companyService) {
        this.equipmentRepository = equipmentRepository;
        this.companyService = companyService;
    }

    public EquipmentDTO createEquipment(EquipmentCreateRequest req) {
        if (req == null || req.companyId() == null) {
            throw new BusinessRuleException("Empresa obrigatória para cadastrar equipamento.");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new BusinessRuleException("O nome do equipamento é obrigatório.");
        }
        if (req.code() == null || req.code().isBlank()) {
            throw new BusinessRuleException("O código do equipamento é obrigatório.");
        }
        if (req.periodValue() <= 0) {
            throw new BusinessRuleException("O prazo de validade do equipamento deve ser superior a zero.");
        }
        Long companyId = QualityEquipmentService.resolveCompanyId(req.companyId());

        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }

        if (equipmentRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, req.code().trim())) {
            throw new BusinessRuleException("Já existe um equipamento cadastrado com o código: " + req.code());
        }

        Equipment eq = new Equipment();
        eq.setCompany(company);
        applyFields(eq, req);
        return mapToEquipmentDTO(equipmentRepository.save(eq));
    }

    public EquipmentDTO updateEquipment(Long id, EquipmentCreateRequest req) {
        if (id == null || req == null || req.companyId() == null) {
            throw new BusinessRuleException("Identificador e empresa obrigatórios.");
        }
        if (req.name() == null || req.name().isBlank() || req.code() == null || req.code().isBlank()) {
            throw new BusinessRuleException("O código e o nome do equipamento são obrigatórios.");
        }
        if (req.periodValue() <= 0) {
            throw new BusinessRuleException("O prazo de validade do equipamento deve ser superior a zero.");
        }
        Long companyId = QualityEquipmentService.resolveCompanyId(req.companyId());
        Equipment eq = equipmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Equipamento não encontrado."));

        if (!eq.getCode().equalsIgnoreCase(req.code().trim())
                && equipmentRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, req.code().trim())) {
            throw new BusinessRuleException("Já existe um equipamento com o código: " + req.code());
        }

        applyFields(eq, req);
        return mapToEquipmentDTO(equipmentRepository.save(eq));
    }

    private void applyFields(Equipment eq, EquipmentCreateRequest req) {
        eq.setCode(req.code().trim().toUpperCase());
        eq.setName(req.name().trim());
        eq.setCategory(req.category() != null ? req.category() : EquipmentCategory.PPE_PROTECTION);
        eq.setPeriodValue(req.periodValue());
        eq.setPeriodUnit(req.periodUnit() != null ? req.periodUnit() : PeriodUnit.MONTHS);
        eq.setWarningDaysBefore(req.warningDaysBefore() > 0 ? req.warningDaysBefore() : 15);
        eq.setActive(req.active());
        eq.setDescription(req.description());
    }

    public EquipmentDTO mapToEquipmentDTO(Equipment e) {
        return new EquipmentDTO(
                e.getId(),
                e.getCompany().getId(),
                e.getCode(),
                e.getName(),
                e.getCategory(),
                e.getCategory() != null ? e.getCategory().getLabel() : "-",
                e.getPeriodValue(),
                e.getPeriodUnit(),
                e.getPeriodUnit() != null ? e.getPeriodUnit().getLabel() : "-",
                e.getWarningDaysBefore(),
                e.isActive(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }
}

package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.OvertimeRateConfigDTO;
import mz.multicore.erp.modules.hr.dto.SaveOvertimeRateConfigRequest;
import mz.multicore.erp.modules.hr.model.OvertimeRateConfig;
import mz.multicore.erp.modules.hr.repository.OvertimeRateConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Configuração dos acréscimos de hora extra. Molde do {@code PayrollTaxConfigService}.
 * Ver docs/RH_COMPLETO_SPEC.md §B2 e §6.
 *
 * <p>Os valores são <b>da empresa</b>, não do sistema: a lei laboral moçambicana tem acréscimos
 * distintos por tipo de hora e cabe ao contabilista confirmar quais se aplicam. O que o sistema
 * garante é que ficam registados com base legal e vigência, e que ninguém paga sobre um número
 * que não foi escolhido por alguém.
 */
@Service
public class OvertimeRateConfigService {

    private final OvertimeRateConfigRepository repository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public OvertimeRateConfigService(OvertimeRateConfigRepository repository,
                                     CompanyRepository companyRepository,
                                     AuditLogService auditLogService) {
        this.repository = repository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<OvertimeRateConfigDTO> list() {
        return repository.findByCompanyIdOrderByEffectiveFromDesc(currentCompanyId())
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public OvertimeRateConfigDTO create(SaveOvertimeRateConfigRequest request) {
        ensureHrManager();
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BusinessRuleException("O fim da vigência não pode ser anterior ao início.");
        }
        OvertimeRateConfig config = new OvertimeRateConfig();
        config.setCompany(companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        config.setName(request.name().trim());
        config.setEffectiveFrom(request.effectiveFrom());
        config.setEffectiveTo(request.effectiveTo());
        config.setDayMultiplier(request.dayMultiplier());
        config.setNightMultiplier(request.nightMultiplier());
        config.setRestDayMultiplier(request.restDayMultiplier());
        config.setLegalBasis(request.legalBasis() == null || request.legalBasis().isBlank()
                ? null : request.legalBasis().trim());
        config.setActive(true);

        OvertimeRateConfig saved = repository.save(config);
        auditLogService.logCurrent("OVERTIME_CONFIG_SAVE", String.format(
                "Acréscimos de hora extra \"%s\" (desde %s): diurna ×%s, nocturna ×%s, descanso ×%s. Base: %s",
                saved.getName(), saved.getEffectiveFrom(), saved.getDayMultiplier(),
                saved.getNightMultiplier(), saved.getRestDayMultiplier(),
                saved.getLegalBasis() == null ? "não indicada" : saved.getLegalBasis()));
        return toDTO(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        ensureHrManager();
        OvertimeRateConfig config = repository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Configuração não encontrada."));
        config.setActive(false);
        repository.save(config);
        auditLogService.logCurrent("OVERTIME_CONFIG_DEACTIVATE",
                "Acréscimos de hora extra \"" + config.getName() + "\" desactivados");
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem configurar acréscimos de hora extra.");
        }
    }

    private OvertimeRateConfigDTO toDTO(OvertimeRateConfig c) {
        return new OvertimeRateConfigDTO(c.getId(), c.getName(), c.getEffectiveFrom(), c.getEffectiveTo(),
                c.getDayMultiplier(), c.getNightMultiplier(), c.getRestDayMultiplier(),
                c.getLegalBasis(), c.isActive());
    }
}

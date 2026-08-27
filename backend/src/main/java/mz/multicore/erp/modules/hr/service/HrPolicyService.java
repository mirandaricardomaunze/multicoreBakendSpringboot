package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.HrPolicyConfigDTO;
import mz.multicore.erp.modules.hr.dto.SaveHrPolicyConfigRequest;
import mz.multicore.erp.modules.hr.model.HrPolicyConfig;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityType;
import mz.multicore.erp.modules.hr.repository.HrPolicyConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Fonte única dos <b>valores legais</b> do RH — direito a férias por antiguidade, prazos de entrega
 * das retenções e aviso prévio. Ver docs/RH_COMPLETO_SPEC.md §6.
 *
 * <p><b>O que este serviço nunca faz é inventar um número.</b> Cada resposta é um
 * {@link Optional}: quem chama tem de decidir explicitamente o que fazer com a ausência, e é isso
 * que impede um valor por omissão de se instalar por acidente e passar despercebido — que foi
 * exactamente como o {@code DEFAULT_ANNUAL_VACATION_DAYS = 22} sobreviveu compilado no código.
 */
@Service
public class HrPolicyService {

    private final HrPolicyConfigRepository repository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public HrPolicyService(HrPolicyConfigRepository repository,
                           CompanyRepository companyRepository,
                           AuditLogService auditLogService) {
        this.repository = repository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<HrPolicyConfigDTO> list() {
        return repository.findByCompanyIdOrderByEffectiveFromDesc(currentCompanyId())
                .stream().map(HrPolicyService::toDTO).toList();
    }

    /** A configuração em vigor numa data. Vazio quando a empresa ainda não configurou nada. */
    @Transactional(readOnly = true)
    public Optional<HrPolicyConfig> applicableOn(LocalDate date) {
        return repository.findApplicable(currentCompanyId(), date).stream().findFirst();
    }

    /**
     * Direito anual de férias para uma antiguidade. Vazio quando não há configuração — e nesse caso
     * quem chama tem de dizer, na resposta ao utilizador, que está a usar um valor por omissão.
     */
    @Transactional(readOnly = true)
    public Optional<Integer> annualVacationDays(int completedYears, LocalDate on) {
        return applicableOn(on)
                .map(config -> config.vacationDaysForSeniority(completedYears));
    }

    /**
     * Prazo de entrega de uma retenção do período {@code year/month}: o dia configurado do mês
     * <b>seguinte</b>. Vazio quando o prazo não está configurado — a obrigação nasce na mesma, sem
     * data, e o painel diz "prazo por configurar" em vez de inventar um.
     */
    @Transactional(readOnly = true)
    public Optional<LocalDate> deliveryDeadline(PayrollLiabilityType type, int year, int month) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        return applicableOn(periodStart)
                .map(config -> type.isSocialSecurity()
                        ? config.getInssDeliveryDay() : config.getIrpsDeliveryDay())
                .map(day -> {
                    LocalDate nextMonth = periodStart.plusMonths(1);
                    return nextMonth.withDayOfMonth(Math.min(day, nextMonth.lengthOfMonth()));
                });
    }

    /** Dias de aviso prévio por iniciativa de cada parte. Vazio quando não está configurado. */
    @Transactional(readOnly = true)
    public Optional<Integer> noticeDays(boolean employerInitiative, LocalDate on) {
        return applicableOn(on).map(config -> employerInitiative
                ? config.getNoticeDaysEmployer() : config.getNoticeDaysEmployee());
    }

    @Transactional
    public HrPolicyConfigDTO create(SaveHrPolicyConfigRequest request) {
        ensureHrManager();
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BusinessRuleException("O fim da vigência não pode ser anterior ao início.");
        }
        HrPolicyConfig config = new HrPolicyConfig();
        config.setCompany(companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        config.setName(request.name().trim());
        config.setEffectiveFrom(request.effectiveFrom());
        config.setEffectiveTo(request.effectiveTo());
        config.setVacationDaysYear1(request.vacationDaysYear1());
        config.setVacationDaysYear2(request.vacationDaysYear2());
        config.setVacationDaysYear3Plus(request.vacationDaysYear3Plus());
        config.setIrpsDeliveryDay(request.irpsDeliveryDay());
        config.setInssDeliveryDay(request.inssDeliveryDay());
        config.setNoticeDaysEmployee(request.noticeDaysEmployee());
        config.setNoticeDaysEmployer(request.noticeDaysEmployer());
        config.setLegalBasis(blankToNull(request.legalBasis()));
        config.setActive(true);

        HrPolicyConfig saved = repository.save(config);
        auditLogService.logCurrent("HR_POLICY_SAVE", String.format(
                "Valores legais de RH \"%s\" (desde %s): férias %s/%s/%s dias, entrega IRPS dia %s, "
                        + "INSS dia %s, aviso prévio %s/%s dias. Base: %s",
                saved.getName(), saved.getEffectiveFrom(),
                orDash(saved.getVacationDaysYear1()), orDash(saved.getVacationDaysYear2()),
                orDash(saved.getVacationDaysYear3Plus()), orDash(saved.getIrpsDeliveryDay()),
                orDash(saved.getInssDeliveryDay()), orDash(saved.getNoticeDaysEmployee()),
                orDash(saved.getNoticeDaysEmployer()),
                saved.getLegalBasis() == null ? "não indicada" : saved.getLegalBasis()));
        return toDTO(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        ensureHrManager();
        HrPolicyConfig config = repository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Configuração não encontrada."));
        config.setActive(false);
        repository.save(config);
        auditLogService.logCurrent("HR_POLICY_DEACTIVATE",
                "Valores legais de RH \"" + config.getName() + "\" desactivados");
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem configurar os valores legais de RH.");
        }
    }

    private static String orDash(Integer value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static HrPolicyConfigDTO toDTO(HrPolicyConfig c) {
        return new HrPolicyConfigDTO(c.getId(), c.getName(), c.getEffectiveFrom(), c.getEffectiveTo(),
                c.getVacationDaysYear1(), c.getVacationDaysYear2(), c.getVacationDaysYear3Plus(),
                c.getIrpsDeliveryDay(), c.getInssDeliveryDay(),
                c.getNoticeDaysEmployee(), c.getNoticeDaysEmployer(),
                c.getLegalBasis(), c.isActive());
    }
}

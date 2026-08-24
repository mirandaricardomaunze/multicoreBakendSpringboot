package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.events.PayrollLiabilityDeliveredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.PayrollCostDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.model.PayrollLiability;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityStatus;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityType;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.PayrollLiabilityRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * As retenções da folha — o dinheiro que a empresa reteve e ainda não entregou.
 * Ver docs/RH_COMPLETO_SPEC.md §B5.
 *
 * <p><b>O buraco que este serviço fecha:</b> o IRPS do trabalhador e o INSS das duas partes eram
 * calculados, somados no mapa fiscal e nunca mais tocados. Só o líquido saía da tesouraria, pelo
 * que o dinheiro do Estado ficava na conta da empresa <b>indistinguível de dinheiro próprio</b>.
 * Não havia obrigação registada, não havia saída, não havia aviso de prazo.
 *
 * <p>A obrigação nasce quando o recibo é <b>pago</b> — é no pagamento que a retenção acontece — e
 * é sempre <b>por período e por tipo</b>, porque é assim que se entrega ao Estado.
 */
@Service
public class PayrollLiabilityService {

    /** Antecedência com que um prazo de entrega passa a ser avisado. */
    public static final int DUE_ALERT_DAYS = 7;

    private final PayrollLiabilityRepository liabilityRepository;
    private final PayslipRepository payslipRepository;
    private final CompanyRepository companyRepository;
    private final HrPolicyService hrPolicyService;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public PayrollLiabilityService(PayrollLiabilityRepository liabilityRepository,
                                   PayslipRepository payslipRepository,
                                   CompanyRepository companyRepository,
                                   HrPolicyService hrPolicyService,
                                   @Lazy FinanceService financeService,
                                   AuditLogService auditLogService,
                                   ApplicationEventPublisher eventPublisher) {
        this.liabilityRepository = liabilityRepository;
        this.payslipRepository = payslipRepository;
        this.companyRepository = companyRepository;
        this.hrPolicyService = hrPolicyService;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    // ─── Apuramento ───────────────────────────────────────────────────────────

    /**
     * Reapura as três obrigações de um período a partir dos recibos <b>já pagos</b>.
     *
     * <p>Reapurar (em vez de somar recibo a recibo) é o que torna isto idempotente: pagar o mesmo
     * recibo duas vezes não pode duplicar a dívida ao Estado, e pagar o 13.º colaborador em atraso
     * tem de aumentar a obrigação sem criar uma segunda linha para o mesmo período.
     *
     * <p><b>Uma obrigação já entregue não se mexe.</b> Se aparecer um recibo novo num período já
     * entregue, o pagamento é recusado nomeando o período: alterar em silêncio um valor que já foi
     * declarado ao Estado é pior do que não deixar pagar.
     */
    @Transactional
    public List<PayrollLiabilityDTO> accrueForPeriod(int year, int month) {
        Long companyId = currentCompanyId();
        List<Payslip> paid = payslipRepository.findByCompanyIdAndYearAndMonth(companyId, year, month)
                .stream().filter(p -> "PAID".equals(p.getStatus())).toList();

        List<PayrollLiabilityDTO> result = new ArrayList<>();
        result.add(upsert(companyId, year, month, PayrollLiabilityType.IRPS,
                sum(paid, Payslip::getIrpsDeduction)));
        result.add(upsert(companyId, year, month, PayrollLiabilityType.INSS_TRABALHADOR,
                sum(paid, Payslip::getInssDeduction)));
        result.add(upsert(companyId, year, month, PayrollLiabilityType.INSS_PATRONAL,
                sum(paid, Payslip::getEmployerInss)));
        return result;
    }

    private PayrollLiabilityDTO upsert(Long companyId, int year, int month,
                                       PayrollLiabilityType type, BigDecimal amount) {
        Optional<PayrollLiability> existing = liabilityRepository
                .findByCompanyIdAndYearAndMonthAndLiabilityType(companyId, year, month, type);

        if (existing.isPresent() && !existing.get().isPending()) {
            PayrollLiability delivered = existing.get();
            if (delivered.getAmount().compareTo(amount) != 0) {
                throw new BusinessRuleException(String.format(
                        "O %s de %d/%d já foi entregue ao Estado (%s). Um recibo novo neste período "
                                + "mudaria um valor já declarado — emita-o no período corrente ou "
                                + "corrija a entrega antes de continuar.",
                        type.getLabel(), month, year, delivered.getAmount()));
            }
            return toDTO(delivered);
        }

        PayrollLiability liability = existing.orElseGet(() -> {
            PayrollLiability fresh = new PayrollLiability();
            fresh.setCompany(companyRepository.findById(companyId)
                    .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
            fresh.setYear(year);
            fresh.setMonth(month);
            fresh.setLiabilityType(type);
            fresh.setStatus(PayrollLiabilityStatus.POR_ENTREGAR);
            return fresh;
        });
        liability.setAmount(amount);
        // O prazo é reavaliado a cada apuramento: configurá-lo depois do primeiro pagamento tem de
        // datar as obrigações que já existem, senão ficavam sem prazo para sempre.
        liability.setDueDate(hrPolicyService.deliveryDeadline(type, year, month).orElse(null));
        return toDTO(liabilityRepository.save(liability));
    }

    // ─── Entrega ──────────────────────────────────────────────────────────────

    /**
     * Marca uma retenção como entregue: saída de tesouraria pela mesma porta do recibo, auditoria e
     * lançamento contabilístico por evento. Espelho exacto do {@code markPayslipPaid}.
     *
     * <p>Entregar duas vezes é recusado — é a forma mais fácil de a empresa pagar ao Estado a
     * dobrar e só dar por isso na reconciliação.
     */
    @Transactional
    public PayrollLiabilityDTO markDelivered(Long id, String paymentReference) {
        ensureHrManager();
        PayrollLiability liability = liabilityRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Retenção não encontrada."));
        if (!liability.isPending()) {
            throw new BusinessRuleException(String.format(
                    "O %s de %d/%d já foi entregue a %s%s.",
                    liability.getLiabilityType().getLabel(), liability.getMonth(), liability.getYear(),
                    liability.getPaymentDate(),
                    liability.getPaymentReference() == null
                            ? "" : " (ref. " + liability.getPaymentReference() + ")"));
        }
        if (liability.getAmount().signum() <= 0) {
            throw new BusinessRuleException("Não há valor a entregar nesta retenção.");
        }

        liability.setStatus(PayrollLiabilityStatus.ENTREGUE);
        liability.setPaymentDate(LocalDate.now());
        liability.setPaymentReference(blankToNull(paymentReference));
        liability.setDeliveredBy(CurrentUserContext.getUsername());
        PayrollLiability saved = liabilityRepository.save(liability);

        String description = String.format("Entrega %s %d/%d",
                saved.getLiabilityType().getLabel(), saved.getMonth(), saved.getYear());
        financeService.registerAutoPayout(saved.getAmount(), description);
        eventPublisher.publishEvent(new PayrollLiabilityDeliveredEvent(
                currentCompanyId(), saved.getId(), saved.getLiabilityType().name(),
                saved.getYear(), saved.getMonth(), saved.getPaymentDate(), saved.getAmount(),
                saved.getPaymentReference()));
        auditLogService.logCurrent("PAYROLL_LIABILITY_DELIVERED", String.format(
                "%s de %d/%d entregue: %s%s",
                saved.getLiabilityType().getLabel(), saved.getMonth(), saved.getYear(),
                saved.getAmount(),
                saved.getPaymentReference() == null ? "" : " (ref. " + saved.getPaymentReference() + ")"));
        return toDTO(saved);
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PayrollLiabilityDTO> list() {
        return liabilityRepository.findAllByCompany(currentCompanyId()).stream()
                .map(PayrollLiabilityService::toDTO).toList();
    }

    /**
     * As retenções que merecem aviso: em atraso, a menos de {@value #DUE_ALERT_DAYS} dias do prazo,
     * ou <b>sem prazo configurado</b>. A terceira é a que interessa não esconder — uma obrigação sem
     * data nunca chega a estar atrasada, e por isso nunca apareceria em lista nenhuma.
     */
    @Transactional(readOnly = true)
    public List<PayrollLiabilityDTO> dueAlerts() {
        LocalDate today = LocalDate.now();
        return liabilityRepository.findByStatus(currentCompanyId(), PayrollLiabilityStatus.POR_ENTREGAR)
                .stream()
                .filter(l -> l.getAmount().signum() > 0)
                .filter(l -> {
                    Long days = l.daysUntilDue(today);
                    return days == null || days <= DUE_ALERT_DAYS;
                })
                .map(PayrollLiabilityService::toDTO)
                .toList();
    }

    /**
     * Custo total do trabalhador no período — base + subsídios + extra + INSS patronal (RHC-55).
     * O patronal é custo da empresa e não aparecia em relatório nenhum: quem olhava para a folha
     * via o ilíquido e pensava que era o que a empresa gasta.
     */
    @Transactional(readOnly = true)
    public PayrollCostDTO monthlyCost(int year, int month) {
        List<Payslip> payslips = payslipRepository
                .findByCompanyIdAndYearAndMonth(currentCompanyId(), year, month).stream()
                .filter(p -> !"CANCELLED".equals(p.getStatus())).toList();

        List<PayrollCostDTO.PayrollCostLineDTO> lines = payslips.stream().map(p -> {
            BigDecimal gross = gross(p);
            return new PayrollCostDTO.PayrollCostLineDTO(
                    p.getEmployee().getId(), p.getEmployee().getEmployeeNumber(),
                    p.getEmployee().getName(), p.getBaseSalary(), p.getAllowances(), p.getOvertime(),
                    gross, p.getEmployerInss(), gross.add(p.getEmployerInss()), p.getNetPay());
        }).toList();

        BigDecimal gross = lines.stream().map(PayrollCostDTO.PayrollCostLineDTO::grossPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal employerInss = lines.stream().map(PayrollCostDTO.PayrollCostLineDTO::employerInss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = lines.stream().map(PayrollCostDTO.PayrollCostLineDTO::netPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayrollCostDTO(year, month, gross, employerInss, gross.add(employerInss), net, lines);
    }

    // ─── Apoio ────────────────────────────────────────────────────────────────

    private static BigDecimal gross(Payslip p) {
        return p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
    }

    private static BigDecimal sum(List<Payslip> payslips, Function<Payslip, BigDecimal> field) {
        return payslips.stream()
                .map(p -> field.apply(p) == null ? BigDecimal.ZERO : field.apply(p))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem entregar retenções da folha.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static PayrollLiabilityDTO toDTO(PayrollLiability l) {
        LocalDate today = LocalDate.now();
        return new PayrollLiabilityDTO(
                l.getId(), l.getYear(), l.getMonth(),
                l.getLiabilityType().name(), l.getLiabilityType().getLabel(),
                l.getAmount(), l.getDueDate(), l.daysUntilDue(today), l.isOverdue(today),
                l.getStatus().name(), l.getStatus().getLabel(),
                l.getPaymentDate(), l.getPaymentReference(), l.getDeliveredBy());
    }
}

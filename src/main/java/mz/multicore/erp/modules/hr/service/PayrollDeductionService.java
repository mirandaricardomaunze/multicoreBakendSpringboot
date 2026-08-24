package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreatePayrollDeductionRequest;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDeductionLineDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.PayrollDeduction;
import mz.multicore.erp.modules.hr.model.PayrollDeductionKind;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.model.PayslipDeductionLine;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayrollDeductionRepository;
import mz.multicore.erp.modules.hr.repository.PayslipDeductionLineRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Descontos recorrentes, adiantamentos e empréstimos ao colaborador.
 * Ver docs/RH_COMPLETO_SPEC.md §B6.
 *
 * <p><b>Os dois defeitos que fecha:</b> um adiantamento <b>saía da caixa e nunca voltava</b>, porque
 * nada o ligava ao recibo do período; e o recibo mostrava um único {@code otherDeductions} anónimo,
 * que é a origem clássica da reclamação do trabalhador — o líquido desce e ninguém sabe dizer
 * porquê.
 *
 * <p><b>O saldo em dívida nunca é gravado</b>: apura-se das linhas aplicadas. É o mesmo raciocínio
 * dos totais do ponto e da caducidade da cotação — um saldo em coluna própria é uma segunda verdade
 * e desactualiza-se à primeira anulação de recibo.
 */
@Service
public class PayrollDeductionService {

    private final PayrollDeductionRepository deductionRepository;
    private final PayslipDeductionLineRepository lineRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;

    public PayrollDeductionService(PayrollDeductionRepository deductionRepository,
                                   PayslipDeductionLineRepository lineRepository,
                                   EmployeeRepository employeeRepository,
                                   CompanyRepository companyRepository,
                                   @Lazy FinanceService financeService,
                                   AuditLogService auditLogService) {
        this.deductionRepository = deductionRepository;
        this.lineRepository = lineRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
    }

    // ─── Criar compromissos ───────────────────────────────────────────────────

    /**
     * Cria o compromisso. Um adiantamento e um empréstimo <b>saem da tesouraria na hora</b> — é
     * dinheiro entregue ao colaborador — e é isso que os liga ao desconto que se segue. Um desconto
     * recorrente não entrega nada, por isso não sai nada.
     */
    @Transactional
    public PayrollDeductionDTO create(CreatePayrollDeductionRequest request) {
        ensureHrManager();
        Employee employee = findEmployee(request.employeeId());
        PayrollDeductionKind kind = parseKind(request.kind());
        validate(kind, request);

        PayrollDeduction deduction = new PayrollDeduction();
        deduction.setCompany(companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        deduction.setEmployee(employee);
        deduction.setKind(kind);
        deduction.setDescription(request.description().trim());
        deduction.setPrincipalAmount(principalFor(kind, request));
        deduction.setInstallments(installmentsFor(kind, request));
        deduction.setInstallmentAmount(installmentAmountFor(kind, request));
        deduction.setStartDate(request.startDate());
        deduction.setEndDate(request.endDate());
        deduction.setActive(true);
        deduction.setNotes(blankToNull(request.notes()));

        PayrollDeduction saved = deductionRepository.save(deduction);

        if (kind.paysOutOnCreation()) {
            financeService.registerAutoPayout(saved.getPrincipalAmount(), String.format(
                    "%s a %s - %s", kind.getLabel(), employee.getName(), saved.getDescription()));
            saved.setPaidOut(true);
            deductionRepository.save(saved);
        }
        auditLogService.logCurrent("PAYROLL_DEDUCTION_CREATE", String.format(
                "%s de %s para %s: %s, %s prestação(ões) de %s a partir de %s",
                kind.getLabel(), saved.getPrincipalAmount() == null ? "valor variável" : saved.getPrincipalAmount(),
                employee.getName(), saved.getDescription(),
                saved.getInstallments() == null ? "sem limite de" : saved.getInstallments(),
                saved.getInstallmentAmount(), saved.getStartDate()));
        return toDTO(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        ensureHrManager();
        PayrollDeduction deduction = deductionRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Desconto não encontrado."));
        deduction.setActive(false);
        deductionRepository.save(deduction);
        auditLogService.logCurrent("PAYROLL_DEDUCTION_DEACTIVATE", String.format(
                "Desconto \"%s\" de %s desactivado com %s por liquidar",
                deduction.getDescription(), deduction.getEmployee().getName(),
                deduction.outstanding(applied(deduction))));
    }

    // ─── Aplicação ao recibo ──────────────────────────────────────────────────

    /**
     * Aplica ao recibo os descontos vigentes no período e devolve o total.
     *
     * <p><b>Nunca desconta mais do que sobra.</b> Se o salário não chegar para tudo, leva o que há e
     * o resto <b>continua em dívida</b> — não é erro, é a realidade: não se pode tirar a um
     * colaborador dinheiro que ele não recebeu. E a ordem é a mais antiga primeiro, para o líquido
     * ser previsível em vez de depender do que a base de dados devolver.
     */
    @Transactional
    public BigDecimal applyTo(Payslip payslip, BigDecimal availableAmount) {
        LocalDate periodEnd = periodEnd(payslip);
        BigDecimal remainingRoom = availableAmount == null ? BigDecimal.ZERO : availableAmount;
        BigDecimal total = BigDecimal.ZERO;

        for (PayrollDeduction deduction : deductionRepository.findApplicable(
                payslip.getEmployee().getId(), currentCompanyId(), periodEnd)) {
            if (remainingRoom.signum() <= 0) {
                break;
            }
            BigDecimal alreadyApplied = applied(deduction);
            if (deduction.isSettled(alreadyApplied)) {
                continue;
            }
            BigDecimal amount = deduction.getInstallmentAmount()
                    .min(deduction.outstanding(alreadyApplied))
                    .min(remainingRoom);
            if (amount.signum() <= 0) {
                continue;
            }

            PayslipDeductionLine line = new PayslipDeductionLine();
            line.setPayslip(payslip);
            line.setDeduction(deduction);
            line.setDescription(deduction.getDescription());
            line.setAmount(amount);
            lineRepository.save(line);

            total = total.add(amount);
            remainingRoom = remainingRoom.subtract(amount);
        }
        return total;
    }

    /**
     * Liberta as linhas de um recibo anulado. Sem isto, um empréstimo ficava pago com dinheiro que
     * ninguém chegou a descontar — e o colaborador ficava a dever menos do que deve.
     */
    @Transactional
    public void releaseFromPayslip(Long payslipId) {
        lineRepository.deleteByPayslipId(payslipId);
    }

    @Transactional(readOnly = true)
    public List<PayslipDeductionLineDTO> linesOf(Long payslipId) {
        return lineRepository.findByPayslipId(payslipId).stream()
                .map(l -> new PayslipDeductionLineDTO(l.getId(), l.getDeduction().getId(),
                        l.getDeduction().getKind().name(), l.getDeduction().getKind().getLabel(),
                        l.getDescription(), l.getAmount()))
                .toList();
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PayrollDeductionDTO> list() {
        return deductionRepository.findAllByCompany(currentCompanyId()).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollDeductionDTO> listForEmployee(Long employeeId) {
        return deductionRepository.findByEmployee(employeeId, currentCompanyId())
                .stream().map(this::toDTO).toList();
    }

    /**
     * O que um colaborador ainda deve à empresa. É o que <b>entra no acerto final</b> (B3): quem sai
     * com um empréstimo a meio não pode levar o saldo consigo sem que ninguém o abata.
     */
    @Transactional(readOnly = true)
    public List<PayrollDeductionDTO> outstandingFor(Long employeeId) {
        return listForEmployee(employeeId).stream()
                .filter(d -> !d.settled() && d.outstandingAmount().signum() > 0)
                .filter(PayrollDeductionDTO::paidOut)
                .toList();
    }

    // ─── Invariantes e apoio ──────────────────────────────────────────────────

    private void validate(PayrollDeductionKind kind, CreatePayrollDeductionRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("O fim do desconto não pode ser anterior ao início.");
        }
        if (kind != PayrollDeductionKind.RECORRENTE && request.principalAmount() == null) {
            throw new BusinessRuleException(String.format(
                    "Um %s tem de indicar o valor entregue ao colaborador.", kind.getLabel().toLowerCase()));
        }
        if (kind == PayrollDeductionKind.RECORRENTE
                && request.principalAmount() == null && request.installmentAmount() == null) {
            throw new BusinessRuleException(
                    "Um desconto recorrente tem de indicar quanto desconta em cada recibo.");
        }
        if (kind == PayrollDeductionKind.ADIANTAMENTO && request.installments() != null
                && request.installments() > 1) {
            throw new BusinessRuleException(
                    "Um adiantamento desconta-se de uma vez. Para prestações, registe um empréstimo.");
        }
    }

    private BigDecimal principalFor(PayrollDeductionKind kind, CreatePayrollDeductionRequest request) {
        // Um recorrente sem fim não tem capital: tem vigência. Inventar-lhe um capital fá-lo-ia
        // "saldar-se" sozinho e parar de descontar sem ninguém ter decidido isso.
        if (kind == PayrollDeductionKind.RECORRENTE && request.endDate() == null) {
            return null;
        }
        return request.principalAmount();
    }

    private Integer installmentsFor(PayrollDeductionKind kind, CreatePayrollDeductionRequest request) {
        // Integer.valueOf(1), e não 1: com um int num dos ramos, o ternário passa a ser de tipo
        // int e desembrulha o outro ramo — um recorrente sem prestações rebentava com NPE aqui.
        return kind == PayrollDeductionKind.ADIANTAMENTO ? Integer.valueOf(1) : request.installments();
    }

    private BigDecimal installmentAmountFor(PayrollDeductionKind kind,
                                            CreatePayrollDeductionRequest request) {
        if (kind == PayrollDeductionKind.ADIANTAMENTO) {
            return request.principalAmount();
        }
        if (request.installmentAmount() != null && request.installmentAmount().signum() > 0) {
            return request.installmentAmount();
        }
        if (request.principalAmount() != null && request.installments() != null) {
            // Arredonda para cima, para a última prestação ser a mais pequena. Ao contrário: a
            // última ficaria com todos os cêntimos que faltassem, e é sempre essa que dá discussão.
            return request.principalAmount().divide(
                    BigDecimal.valueOf(request.installments()), 2, RoundingMode.CEILING);
        }
        throw new BusinessRuleException("Indique o valor de cada prestação ou o número de prestações.");
    }

    private BigDecimal applied(PayrollDeduction deduction) {
        BigDecimal sum = lineRepository.sumApplied(deduction.getId());
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private static LocalDate periodEnd(Payslip payslip) {
        LocalDate first = LocalDate.of(payslip.getYear(), payslip.getMonth(), 1);
        return first.withDayOfMonth(first.lengthOfMonth());
    }

    private PayrollDeductionKind parseKind(String raw) {
        try {
            return PayrollDeductionKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de desconto desconhecido: " + raw);
        }
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem gerir descontos e adiantamentos.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PayrollDeductionDTO toDTO(PayrollDeduction d) {
        BigDecimal applied = applied(d);
        return new PayrollDeductionDTO(d.getId(), d.getEmployee().getId(), d.getEmployee().getName(),
                d.getKind().name(), d.getKind().getLabel(), d.getDescription(),
                d.getPrincipalAmount(), d.getInstallmentAmount(), d.getInstallments(),
                d.getStartDate(), d.getEndDate(), applied, d.outstanding(applied),
                d.isSettled(applied), d.isPaidOut(), d.isActive(), d.getNotes());
    }
}

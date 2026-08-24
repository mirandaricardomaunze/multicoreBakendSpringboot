package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SalaryChange;
import mz.multicore.erp.modules.hr.model.SalaryChangeReason;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.SalaryChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Histórico salarial. Ver docs/RH_COMPLETO_SPEC.md §B4.
 *
 * <p><b>O salário não é um número, é uma série datada.</b> Antes disto, alterá-lo sobrepunha o
 * anterior: perdia-se o valor antigo, quem o mudou, porquê e desde quando. A consequência séria não
 * era o histórico perdido — era o <b>recibo de Março passar a pagar ao valor de Setembro</b>
 * quando alguém o reprocessasse, sem nada parecer errado.
 */
@Service
public class SalaryHistoryService {

    private final SalaryChangeRepository salaryChangeRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public SalaryHistoryService(SalaryChangeRepository salaryChangeRepository,
                                EmployeeRepository employeeRepository,
                                CompanyRepository companyRepository,
                                AuditLogService auditLogService) {
        this.salaryChangeRepository = salaryChangeRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    /** A série completa de um colaborador, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<SalaryChangeDTO> getHistory(Long employeeId) {
        return salaryChangeRepository.findByEmployee(employeeId, currentCompanyId()).stream()
                .map(this::toDTO).toList();
    }

    /**
     * O salário vigente numa data. <b>Fonte única</b> da pergunta que o recibo faz — sem isto,
     * "o salário" é só o último número escrito e o passado deixa de ser reconstituível.
     *
     * <p>Vazio quando não há histórico nenhum (colaborador anterior a este bloco): nesse caso quem
     * chama fica com o valor da ficha, que é o comportamento de sempre.
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> salaryOn(Long employeeId, LocalDate date) {
        return salaryChangeRepository.findEffectiveOn(employeeId, currentCompanyId(), date)
                .stream().findFirst().map(SalaryChange::getNewSalary);
    }

    @Transactional
    public SalaryChangeDTO registerChange(CreateSalaryChangeRequest request) {
        ensureHrManager();
        Employee employee = findEmployee(request.employeeId());
        SalaryChangeReason reason = parseReason(request.reason());
        return record(employee, request.newSalary(), request.effectiveDate(), reason,
                blankToNull(request.jobTitle()), blankToNull(request.department()),
                blankToNull(request.notes()));
    }

    /**
     * Grava uma alteração. Usado pelo registo manual e pela activação de contrato — o salário
     * acordado num contrato que passa a vigorar <b>é</b> uma alteração salarial, e fingir que não é
     * deixaria buracos na série exactamente nos momentos que mais interessam.
     */
    @Transactional
    public SalaryChangeDTO record(Employee employee, BigDecimal newSalary, LocalDate effectiveDate,
                                  SalaryChangeReason reason, String jobTitle, String department,
                                  String notes) {
        Long companyId = currentCompanyId();
        BigDecimal previous = salaryOn(employee.getId(), effectiveDate)
                .orElse(employee.getBaseSalary());

        if (previous != null && previous.compareTo(newSalary) == 0) {
            throw new BusinessRuleException(String.format(
                    "O salário de %s já é %s nessa data — não há alteração a registar.",
                    employee.getName(), newSalary));
        }
        if (salaryChangeRepository.existsByCompanyIdAndEmployeeIdAndEffectiveDate(
                companyId, employee.getId(), effectiveDate)) {
            throw new BusinessRuleException(String.format(
                    "%s já tem uma alteração salarial com efeito a %s.",
                    employee.getName(), effectiveDate));
        }

        SalaryChange change = new SalaryChange();
        change.setEmployee(employee);
        change.setCompany(companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        change.setPreviousSalary(previous);
        change.setNewSalary(newSalary);
        change.setEffectiveDate(effectiveDate);
        change.setReason(reason);
        change.setJobTitle(jobTitle);
        change.setDepartment(department);
        change.setApprovedBy(CurrentUserContext.getUsername());
        change.setNotes(notes);

        SalaryChange saved = salaryChangeRepository.save(change);
        applyToEmployeeCardIfCurrent(employee, saved);

        auditLogService.logCurrent("SALARY_CHANGE", String.format(
                "Salário de %s: %s → %s com efeito a %s (%s)%s",
                employee.getName(), previous == null ? "sem registo" : previous, newSalary,
                effectiveDate, reason.getLabel(), notes == null ? "" : ". " + notes));
        return toDTO(saved);
    }

    /**
     * A ficha do colaborador continua a mostrar o salário actual — mas passa a ser <b>reflexo</b> da
     * série, não a origem dela. Uma alteração com data futura não mexe na ficha até lá chegar.
     */
    private void applyToEmployeeCardIfCurrent(Employee employee, SalaryChange change) {
        if (change.appliesOn(LocalDate.now())) {
            employee.setBaseSalary(change.getNewSalary());
            if (change.getJobTitle() != null) {
                employee.setRole(change.getJobTitle());
            }
            if (change.getDepartment() != null) {
                employee.setDepartment(change.getDepartment());
            }
            employeeRepository.save(employee);
        }
    }

    private SalaryChangeReason parseReason(String raw) {
        try {
            return SalaryChangeReason.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Motivo de alteração salarial desconhecido: " + raw);
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
                    "Apenas gestores ou administradores podem alterar salários.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SalaryChangeDTO toDTO(SalaryChange c) {
        BigDecimal difference = c.getPreviousSalary() == null
                ? null : c.getNewSalary().subtract(c.getPreviousSalary());
        return new SalaryChangeDTO(c.getId(), c.getEmployee().getId(), c.getEmployee().getName(),
                c.getPreviousSalary(), c.getNewSalary(), difference, c.getEffectiveDate(),
                c.getReason().name(), c.getReason().getLabel(), c.getJobTitle(), c.getDepartment(),
                c.getApprovedBy(), c.getNotes());
    }
}

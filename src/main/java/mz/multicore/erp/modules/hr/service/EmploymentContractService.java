package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import mz.multicore.erp.modules.hr.model.ContractStatus;
import mz.multicore.erp.modules.hr.model.ContractType;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.SalaryChangeReason;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contratos de trabalho. Serviço próprio, e não mais um bloco no {@code HRService}: o ciclo de vida
 * do contrato (criar → vigorar → renovar → cessar) é um assunto inteiro, com invariantes suas.
 *
 * <p>A invariante central é <b>um só contrato vigente numa data</b>. Sem ela, "qual é o salário
 * deste colaborador em Agosto?" deixa de ter resposta única — e é essa pergunta que a folha faz.
 * Ver docs/RH_COMPLETO_SPEC.md §B1.
 */
@Service
public class EmploymentContractService {

    /** Antecedência com que um fim de contrato passa a ser avisado. */
    public static final int CONTRACT_ENDING_ALERT_DAYS = 30;
    /** Antecedência com que um fim de período experimental passa a ser avisado. */
    public static final int PROBATION_ENDING_ALERT_DAYS = 7;

    private final EmploymentContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final DocumentNumberService documentNumberService;
    private final SalaryHistoryService salaryHistoryService;
    private final AuditLogService auditLogService;

    public EmploymentContractService(EmploymentContractRepository contractRepository,
                                     EmployeeRepository employeeRepository,
                                     CompanyRepository companyRepository,
                                     DocumentNumberService documentNumberService,
                                     SalaryHistoryService salaryHistoryService,
                                     AuditLogService auditLogService) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.documentNumberService = documentNumberService;
        this.salaryHistoryService = salaryHistoryService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<EmploymentContractDTO> getAllContracts() {
        return contractRepository.findAllWithEmployeeByCompanyId(currentCompanyId()).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EmploymentContractDTO> getContractsOfEmployee(Long employeeId) {
        return contractRepository.findByEmployeeId(employeeId, currentCompanyId()).stream()
                .map(this::toDTO).toList();
    }

    /**
     * O contrato vigente que cobre qualquer dia de um intervalo. É o que a folha mensal pergunta:
     * quem tem contrato até dia 15 trabalhou meio mês e tem de ser pago — usar só o último dia do
     * mês fá-lo-ia desaparecer da folha.
     */
    @Transactional(readOnly = true)
    public Optional<EmploymentContract> findContractInPeriod(Long employeeId, LocalDate from, LocalDate to) {
        return contractRepository.findOverlapping(employeeId, currentCompanyId(), from, to, -1L)
                .stream().findFirst();
    }

    @Transactional
    public EmploymentContractDTO createContract(CreateContractRequest request) {
        ensureHrManager();
        Employee employee = findEmployee(request.employeeId());
        ContractType type = parseType(request.contractType());
        validateDates(type, request.startDate(), request.endDate(), request.probationEndDate(),
                request.termReason());

        EmploymentContract contract = new EmploymentContract();
        contract.setEmployee(employee);
        contract.setCompany(currentCompany());
        contract.setContractNumber(documentNumberService.next(DocumentSeries.EMPLOYMENT_CONTRACT));
        contract.setContractType(type);
        contract.setStatus(ContractStatus.RASCUNHO);
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setProbationEndDate(request.probationEndDate());
        contract.setAgreedSalary(request.agreedSalary());
        contract.setWeeklyHours(request.weeklyHours() <= 0 ? 40 : request.weeklyHours());
        contract.setJobTitle(request.jobTitle().trim());
        contract.setWorkLocation(blankToNull(request.workLocation()));
        contract.setTermReason(blankToNull(request.termReason()));

        EmploymentContract saved = contractRepository.save(contract);
        auditLogService.logCurrent("CONTRACT_CREATE", String.format(
                "Contrato %s (%s) criado para %s, início %s",
                saved.getContractNumber(), type.getLabel(), employee.getName(), saved.getStartDate()));
        return toDTO(saved);
    }

    /**
     * Põe o contrato a vigorar. É aqui — e não na criação — que se verifica a sobreposição: um
     * rascunho não manda em nada, dois vigentes sim.
     */
    @Transactional
    public EmploymentContractDTO activateContract(Long id) {
        ensureHrManager();
        EmploymentContract contract = findContract(id);
        if (contract.getStatus() != ContractStatus.RASCUNHO) {
            throw new BusinessRuleException("Só um contrato em rascunho pode passar a vigente.");
        }
        ensureNoOverlap(contract);
        contract.setStatus(ContractStatus.VIGENTE);

        EmploymentContract saved = contractRepository.save(contract);
        // A ficha passa a derivar do contrato vigente: o salário acordado é o que a folha usa.
        syncEmployeeFromContract(saved);
        auditLogService.logCurrent("CONTRACT_ACTIVATE", String.format(
                "Contrato %s de %s passou a vigente (%s a %s)",
                saved.getContractNumber(), saved.getEmployee().getName(),
                saved.getStartDate(), saved.getEndDate() == null ? "sem termo" : saved.getEndDate()));
        return toDTO(saved);
    }

    /**
     * Renovação: contrato <b>novo</b> ligado ao anterior, que fica cessado no dia antes do novo
     * começar. Não se edita o antigo — o que foi acordado no passado não muda.
     */
    @Transactional
    public EmploymentContractDTO renewContract(Long id, RenewContractRequest request) {
        ensureHrManager();
        EmploymentContract previous = findContract(id);
        if (previous.getStatus() == ContractStatus.RASCUNHO) {
            throw new BusinessRuleException("Um contrato em rascunho não se renova — altere-o.");
        }
        if (!request.startDate().isAfter(previous.getStartDate())) {
            throw new BusinessRuleException("A renovação tem de começar depois do contrato anterior.");
        }
        validateDates(previous.getContractType(), request.startDate(), request.endDate(), null,
                request.termReason() == null ? previous.getTermReason() : request.termReason());

        EmploymentContract renewal = new EmploymentContract();
        renewal.setEmployee(previous.getEmployee());
        renewal.setCompany(previous.getCompany());
        renewal.setContractNumber(documentNumberService.next(DocumentSeries.EMPLOYMENT_CONTRACT));
        renewal.setContractType(previous.getContractType());
        renewal.setStatus(ContractStatus.RASCUNHO);
        renewal.setStartDate(request.startDate());
        renewal.setEndDate(request.endDate());
        // O período experimental não se repete numa renovação — o colaborador já foi experimentado.
        renewal.setProbationEndDate(null);
        renewal.setAgreedSalary(request.agreedSalary() == null
                ? previous.getAgreedSalary() : request.agreedSalary());
        renewal.setWeeklyHours(previous.getWeeklyHours());
        renewal.setJobTitle(previous.getJobTitle());
        renewal.setWorkLocation(previous.getWorkLocation());
        renewal.setTermReason(request.termReason() == null
                ? previous.getTermReason() : blankToNull(request.termReason()));
        renewal.setRenewedFrom(previous);

        // O anterior fecha na véspera do novo, para não haver dois vigentes no mesmo dia.
        if (previous.getStatus() == ContractStatus.VIGENTE) {
            previous.setStatus(ContractStatus.CESSADO);
            previous.setTerminationDate(request.startDate().minusDays(1));
            previous.setTerminationReason("Renovado");
            contractRepository.save(previous);
        }

        EmploymentContract saved = contractRepository.save(renewal);
        auditLogService.logCurrent("CONTRACT_RENEW", String.format(
                "Contrato %s de %s renovado por %s (início %s)",
                previous.getContractNumber(), previous.getEmployee().getName(),
                saved.getContractNumber(), saved.getStartDate()));
        return toDTO(saved);
    }

    @Transactional
    public EmploymentContractDTO terminateContract(Long id, LocalDate terminationDate, String reason) {
        ensureHrManager();
        EmploymentContract contract = findContract(id);
        if (contract.getStatus().isTerminal()) {
            throw new BusinessRuleException("Este contrato já está cessado.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("O motivo da cessação é obrigatório.");
        }
        LocalDate effective = terminationDate == null ? LocalDate.now() : terminationDate;
        if (effective.isBefore(contract.getStartDate())) {
            throw new BusinessRuleException("A cessação não pode ser anterior ao início do contrato.");
        }
        contract.setStatus(ContractStatus.CESSADO);
        contract.setTerminationDate(effective);
        contract.setTerminationReason(reason.trim());

        EmploymentContract saved = contractRepository.save(contract);
        auditLogService.logCurrent("CONTRACT_TERMINATE", String.format(
                "Contrato %s de %s cessado em %s: %s",
                saved.getContractNumber(), saved.getEmployee().getName(), effective, reason.trim()));
        return toDTO(saved);
    }

    /**
     * Os dois avisos de contrato, numa só passagem: fim de contrato a ≤30 dias e fim de período
     * experimental a ≤7 dias. Janelas diferentes porque as decisões são diferentes — renovar um
     * contrato tem antecedência, confirmar (ou não) alguém no fim da experiência não tem.
     */
    @Transactional(readOnly = true)
    public ContractAlertsDTO getContractAlerts() {
        LocalDate today = LocalDate.now();
        Long companyId = currentCompanyId();
        List<EmploymentContractDTO> ending = contractRepository.findEndingBetween(
                        companyId, ContractStatus.VIGENTE, today, today.plusDays(CONTRACT_ENDING_ALERT_DAYS))
                .stream().map(this::toDTO).toList();
        List<EmploymentContractDTO> probation = contractRepository.findProbationEndingBetween(
                        companyId, ContractStatus.VIGENTE, today, today.plusDays(PROBATION_ENDING_ALERT_DAYS))
                .stream().map(this::toDTO).toList();
        return new ContractAlertsDTO(ending, probation);
    }

    // ─── Invariantes ──────────────────────────────────────────────────────────

    /**
     * Um só contrato vigente numa data. Sem isto, "qual é o salário deste colaborador em Agosto?"
     * deixa de ter resposta única — e é essa a pergunta que a folha mensal faz.
     */
    private void ensureNoOverlap(EmploymentContract contract) {
        LocalDate rangeEnd = contract.getEndDate() == null ? LocalDate.of(9999, 12, 31) : contract.getEndDate();
        List<EmploymentContract> clashes = contractRepository.findOverlapping(
                contract.getEmployee().getId(), currentCompanyId(),
                contract.getStartDate(), rangeEnd,
                contract.getId() == null ? -1L : contract.getId());
        if (!clashes.isEmpty()) {
            EmploymentContract clash = clashes.get(0);
            throw new BusinessRuleException(String.format(
                    "%s já tem o contrato %s vigente neste período (%s a %s). Cesse-o ou renove-o.",
                    contract.getEmployee().getName(), clash.getContractNumber(), clash.getStartDate(),
                    clash.getEndDate() == null ? "sem termo" : clash.getEndDate()));
        }
    }

    private void validateDates(ContractType type, LocalDate startDate, LocalDate endDate,
                               LocalDate probationEndDate, String termReason) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("O fim do contrato não pode ser anterior ao início.");
        }
        if (type.requiresEndDate() && endDate == null) {
            throw new BusinessRuleException(String.format(
                    "Um contrato %s tem de ter data de fim.", type.getLabel().toLowerCase()));
        }
        if (!type.isFixedTerm() && endDate != null) {
            throw new BusinessRuleException("Um contrato sem termo não pode ter data de fim.");
        }
        // Exigência da lei laboral: um contrato a termo tem de dizer porquê é a termo.
        if (type.isFixedTerm() && (termReason == null || termReason.isBlank())) {
            throw new BusinessRuleException(
                    "Um contrato a termo tem de indicar o motivo do termo (exigência legal).");
        }
        if (probationEndDate != null && probationEndDate.isBefore(startDate)) {
            throw new BusinessRuleException(
                    "O período experimental não pode terminar antes do início do contrato.");
        }
        if (probationEndDate != null && endDate != null && probationEndDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                    "O período experimental não pode terminar depois do contrato.");
        }
    }

    /**
     * O salário acordado num contrato que passa a vigorar <b>é</b> uma alteração salarial, com data
     * de efeito na data de início do contrato. Registá-la (em vez de escrever na ficha à socapa)
     * mantém a série do B4 sem buracos exactamente nos momentos que mais interessam.
     *
     * <p>Se o valor não mudou, não há alteração a registar — e isso não é erro nenhum.
     */
    private void syncEmployeeFromContract(EmploymentContract contract) {
        Employee employee = contract.getEmployee();
        try {
            salaryHistoryService.record(employee, contract.getAgreedSalary(), contract.getStartDate(),
                    SalaryChangeReason.CONTRATO, contract.getJobTitle(), null,
                    "Contrato " + contract.getContractNumber());
        } catch (BusinessRuleException alreadyAtThisSalary) {
            // Salário inalterado face ao vigente: nada a registar.
        }
    }

    // ─── Apoio ────────────────────────────────────────────────────────────────

    private ContractType parseType(String raw) {
        try {
            return ContractType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de contrato desconhecido: " + raw);
        }
    }

    private EmploymentContract findContract(Long id) {
        return contractRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado."));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
    }

    private Company currentCompany() {
        Long companyId = currentCompanyId();
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada."));
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException("Apenas gestores ou administradores podem gerir contratos.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private EmploymentContractDTO toDTO(EmploymentContract c) {
        LocalDate today = LocalDate.now();
        return new EmploymentContractDTO(
                c.getId(), c.getContractNumber(), c.getEmployee().getId(), c.getEmployee().getName(),
                c.getContractType().name(), c.getContractType().getLabel(),
                c.getStatus().name(), c.getStatus().getLabel(),
                c.getStartDate(), c.getEndDate(), c.getProbationEndDate(),
                c.getAgreedSalary(), c.getWeeklyHours(), c.getJobTitle(), c.getWorkLocation(),
                c.getTermReason(),
                c.getRenewedFrom() == null ? null : c.getRenewedFrom().getId(),
                c.getRenewedFrom() == null ? null : c.getRenewedFrom().getContractNumber(),
                c.getTerminationDate(), c.getTerminationReason(),
                c.isExpired(today), c.isInProbation(today), c.daysUntilEnd(today)
        );
    }
}

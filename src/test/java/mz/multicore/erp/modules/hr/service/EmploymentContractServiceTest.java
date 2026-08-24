package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import mz.multicore.erp.modules.hr.model.ContractStatus;
import mz.multicore.erp.modules.hr.model.ContractType;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.SalaryChangeReason;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmploymentContractServiceTest {

    private EmploymentContractRepository contractRepository;
    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private DocumentNumberService documentNumberService;
    private SalaryHistoryService salaryHistoryService;
    private AuditLogService auditLogService;
    private EmploymentContractService service;

    @BeforeEach
    void setUp() {
        contractRepository = mock(EmploymentContractRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        documentNumberService = mock(DocumentNumberService.class);
        salaryHistoryService = mock(SalaryHistoryService.class);
        auditLogService = mock(AuditLogService.class);
        service = new EmploymentContractService(contractRepository, employeeRepository,
                companyRepository, documentNumberService, salaryHistoryService, auditLogService);

        Company company = new Company();
        company.setId(7L);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        when(documentNumberService.next(any())).thenReturn("CTR-2026/1");
        when(contractRepository.save(any(EmploymentContract.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(employee()));
        CurrentUserContext.setCurrentCompanyId(7L);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void employeeRole_cannotCreateContract() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.createContract(request(ContractType.SEM_TERMO)));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void fixedTermContract_withoutReason_isBlocked() {
        // Exigência da lei laboral: um contrato a termo tem de dizer porquê é a termo.
        var request = new CreateContractRequest(5L, "TERMO_CERTO", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), null, new BigDecimal("30000"), 40, "Operador", null, "  ");

        var ex = assertThrows(BusinessRuleException.class, () -> service.createContract(request));
        assertEquals(true, ex.getMessage().contains("motivo do termo"));
    }

    @Test
    void fixedTermContract_withoutEndDate_isBlocked() {
        var request = new CreateContractRequest(5L, "TERMO_CERTO", LocalDate.of(2026, 1, 1),
                null, null, new BigDecimal("30000"), 40, "Operador", null, "Substituição");

        var ex = assertThrows(BusinessRuleException.class, () -> service.createContract(request));
        assertEquals(true, ex.getMessage().contains("data de fim"));
    }

    @Test
    void openEndedContract_withEndDate_isBlocked() {
        var request = new CreateContractRequest(5L, "SEM_TERMO", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), null, new BigDecimal("30000"), 40, "Operador", null, null);

        var ex = assertThrows(BusinessRuleException.class, () -> service.createContract(request));
        assertEquals(true, ex.getMessage().contains("sem termo não pode ter data de fim"));
    }

    @Test
    void termoIncerto_withoutEndDate_isAllowed() {
        // O termo incerto acaba quando a tarefa acaba: a data só se conhece no fim.
        var dto = service.createContract(new CreateContractRequest(5L, "TERMO_INCERTO",
                LocalDate.of(2026, 1, 1), null, null, new BigDecimal("30000"), 40,
                "Operador", null, "Obra certa"));

        assertEquals("TERMO_INCERTO", dto.contractType());
        assertNull(dto.endDate());
    }

    @Test
    void newContract_startsAsDraft() {
        // Um rascunho não manda em nada — só o vigente é que conta para a folha.
        assertEquals("RASCUNHO", service.createContract(request(ContractType.SEM_TERMO)).status());
    }

    @Test
    void activating_whenAnotherContractIsAlreadyCurrent_isBlocked() {
        // A invariante do bloco: um só contrato vigente numa data. Sem ela, "qual é o salário deste
        // colaborador em Agosto?" deixa de ter resposta única.
        EmploymentContract draft = contract(10L, ContractStatus.RASCUNHO,
                LocalDate.of(2026, 3, 1), null);
        EmploymentContract current = contract(3L, ContractStatus.VIGENTE,
                LocalDate.of(2025, 1, 1), null);
        current.setContractNumber("CTR-2025/9");
        when(contractRepository.findByIdWithEmployeeAndCompanyId(10L, 7L)).thenReturn(Optional.of(draft));
        when(contractRepository.findOverlapping(eq(5L), eq(7L), any(), any(), eq(10L)))
                .thenReturn(List.of(current));

        var ex = assertThrows(BusinessRuleException.class, () -> service.activateContract(10L));
        assertEquals(true, ex.getMessage().contains("CTR-2025/9"));
    }

    @Test
    void activating_registersTheAgreedSalaryAsADatedChange() {
        // O salário acordado num contrato que passa a vigorar É uma alteração salarial, com data de
        // efeito no início do contrato (B4). Escrevê-lo na ficha à socapa deixava um buraco na série
        // exactamente no momento que mais interessa.
        EmploymentContract draft = contract(10L, ContractStatus.RASCUNHO, LocalDate.of(2026, 3, 1), null);
        draft.setAgreedSalary(new BigDecimal("42000"));
        when(contractRepository.findByIdWithEmployeeAndCompanyId(10L, 7L)).thenReturn(Optional.of(draft));
        when(contractRepository.findOverlapping(eq(5L), eq(7L), any(), any(), eq(10L))).thenReturn(List.of());

        service.activateContract(10L);

        verify(salaryHistoryService).record(any(), eq(new BigDecimal("42000")),
                eq(LocalDate.of(2026, 3, 1)), eq(SalaryChangeReason.CONTRATO), any(), any(), any());
        verify(auditLogService).logCurrent(eq("CONTRACT_ACTIVATE"), any());
    }

    @Test
    void renewal_createsNewContractAndClosesThePreviousOnTheDayBefore() {
        // O histórico do que foi acordado é imutável: renovar cria contrato novo, não edita o antigo.
        EmploymentContract previous = contract(3L, ContractStatus.VIGENTE,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        previous.setTermReason("Acréscimo de actividade");
        when(contractRepository.findByIdWithEmployeeAndCompanyId(3L, 7L)).thenReturn(Optional.of(previous));
        when(documentNumberService.next(any())).thenReturn("CTR-2026/2");

        var renewal = service.renewContract(3L, new RenewContractRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), new BigDecimal("35000"), null));

        assertEquals("CTR-2026/2", renewal.contractNumber());
        assertEquals(3L, renewal.renewedFromId());
        assertEquals(0, new BigDecimal("35000").compareTo(renewal.agreedSalary()));
        // O anterior fecha na véspera do novo — nunca há dois vigentes no mesmo dia.
        assertEquals(ContractStatus.CESSADO, previous.getStatus());
        assertEquals(LocalDate.of(2025, 12, 31), previous.getTerminationDate());
    }

    @Test
    void renewal_keepsPreviousSalaryWhenNotStated() {
        EmploymentContract previous = contract(3L, ContractStatus.VIGENTE,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        previous.setAgreedSalary(new BigDecimal("28000"));
        previous.setTermReason("Acréscimo de actividade");
        when(contractRepository.findByIdWithEmployeeAndCompanyId(3L, 7L)).thenReturn(Optional.of(previous));

        var renewal = service.renewContract(3L, new RenewContractRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, null));

        assertEquals(0, new BigDecimal("28000").compareTo(renewal.agreedSalary()));
    }

    @Test
    void renewal_startingBeforeThePreviousContract_isBlocked() {
        EmploymentContract previous = contract(3L, ContractStatus.VIGENTE,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        when(contractRepository.findByIdWithEmployeeAndCompanyId(3L, 7L)).thenReturn(Optional.of(previous));

        assertThrows(BusinessRuleException.class, () -> service.renewContract(3L,
                new RenewContractRequest(LocalDate.of(2024, 1, 1), null, null, null)));
    }

    @Test
    void termination_withoutReason_isBlocked() {
        EmploymentContract current = contract(3L, ContractStatus.VIGENTE, LocalDate.of(2025, 1, 1), null);
        when(contractRepository.findByIdWithEmployeeAndCompanyId(3L, 7L)).thenReturn(Optional.of(current));

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.terminateContract(3L, LocalDate.of(2026, 6, 30), "  "));
        assertEquals(true, ex.getMessage().contains("motivo da cessação"));
    }

    @Test
    void termination_isAudited() {
        EmploymentContract current = contract(3L, ContractStatus.VIGENTE, LocalDate.of(2025, 1, 1), null);
        when(contractRepository.findByIdWithEmployeeAndCompanyId(3L, 7L)).thenReturn(Optional.of(current));

        var dto = service.terminateContract(3L, LocalDate.of(2026, 6, 30), "Acordo mútuo");

        assertEquals("CESSADO", dto.status());
        assertEquals(LocalDate.of(2026, 6, 30), dto.terminationDate());
        verify(auditLogService).logCurrent(eq("CONTRACT_TERMINATE"), any());
    }

    @Test
    void expiryIsDerived_neverStored() {
        // Mesma lição da cotação: a caducidade deriva-se da data, não se grava. Um contrato que
        // acabou continua VIGENTE na coluna e sai como caducado no DTO — sem agendador nocturno.
        EmploymentContract ended = contract(3L, ContractStatus.VIGENTE,
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
        when(contractRepository.findAllWithEmployeeByCompanyId(7L)).thenReturn(List.of(ended));

        var dto = service.getAllContracts().get(0);

        assertEquals("VIGENTE", dto.status());
        assertEquals(true, dto.expired());
    }

    @Test
    void lastDayOfContract_stillCounts() {
        // Quem tem contrato "até 31/07" trabalha no dia 31 — e tem de ser pago por ele.
        EmploymentContract contract = contract(3L, ContractStatus.VIGENTE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 31));

        assertEquals(true, contract.coversDate(LocalDate.of(2026, 7, 31)));
        assertEquals(false, contract.coversDate(LocalDate.of(2026, 8, 1)));
    }

    private CreateContractRequest request(ContractType type) {
        return new CreateContractRequest(5L, type.name(), LocalDate.of(2026, 1, 1), null, null,
                new BigDecimal("30000"), 40, "Operador", "Loja da Baixa",
                type.isFixedTerm() ? "Acréscimo de actividade" : null);
    }

    private EmploymentContract contract(Long id, ContractStatus status, LocalDate start, LocalDate end) {
        EmploymentContract c = new EmploymentContract();
        c.setId(id);
        c.setContractNumber("CTR-2026/" + id);
        c.setEmployee(employee());
        c.setCompany(companyRepository.findById(7L).orElseThrow());
        c.setContractType(end == null ? ContractType.SEM_TERMO : ContractType.TERMO_CERTO);
        c.setStatus(status);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setAgreedSalary(new BigDecimal("30000"));
        c.setWeeklyHours(40);
        c.setJobTitle("Operador");
        return c;
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-5");
        employee.setName("Maria");
        employee.setBaseSalary(new BigDecimal("30000"));
        employee.setStatus("ACTIVE");
        return employee;
    }
}

package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.SaveOccupationalHealthExamRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OccupationalHealthServiceTest {
    private OccupationalHealthExamRepository repository;
    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private SupplierRepository supplierRepository;
    private FinanceService financeService;
    private AuditLogService auditLogService;
    private OccupationalHealthService service;
    private Company company;
    private Employee employee;

    @BeforeEach
    void setUp() {
        repository = mock(OccupationalHealthExamRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        financeService = mock(FinanceService.class);
        auditLogService = mock(AuditLogService.class);
        service = new OccupationalHealthService(repository, employeeRepository, companyRepository,
                supplierRepository, financeService, auditLogService);
        CurrentUserContext.setCurrentCompanyId(7L);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        company = new Company();
        company.setId(7L);
        employee = new Employee();
        employee.setId(3L);
        employee.setName("Ana Matola");
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        when(employeeRepository.findByIdAndCompanyId(3L, 7L)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(invocation -> {
            OccupationalHealthExam exam = invocation.getArgument(0);
            if (exam.getId() == null) exam.setId(10L);
            return exam;
        });
    }

    @AfterEach void tearDown() { CurrentUserContext.clear(); }

    @Test
    void register_preservesFitnessValidityAndAttachment() {
        byte[] attachment = {1, 2, 3};
        var result = service.register(request("FIT", null, attachment));

        assertEquals("FIT", result.fitnessResult());
        assertEquals(LocalDate.now().plusYears(1), result.expiryDate());
        assertTrue(result.hasAttachment());
        verify(auditLogService).logCurrent(eq("OCCUPATIONAL_HEALTH_EXAM_REGISTER"), anyString());
    }

    @Test
    void restrictedFitness_requiresRestrictions() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.register(request("FIT_WITH_RESTRICTIONS", " ", null)));
        assertTrue(error.getMessage().contains("restrições"));
        verify(repository, never()).save(any());
    }

    @Test
    void expiryBeforeExamIsRejected() {
        var invalid = new SaveOccupationalHealthExamRequest(3L, "CS-3", LocalDate.now(),
                LocalDate.now().minusDays(1), "FIT", null, null, null, null, null, null, null, null, null);
        assertThrows(BusinessRuleException.class, () -> service.register(invalid));
    }

    @Test
    void employeeCannotReadClinicalHistory() {
        CurrentUserContext.setCurrentUser("trabalhador", "EMPLOYEE");
        assertThrows(BusinessRuleException.class, () -> service.history(3L));
        verify(repository, never()).findHistory(anyLong(), anyLong());
    }

    @Test
    void generalSummaryDoesNotExposeClinicalDetails() {
        when(repository.findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(7L, 3L))
                .thenReturn(Optional.of(exam("FIT_WITH_RESTRICTIONS", LocalDate.now().plusDays(30))));

        var summary = service.summary(3L);

        assertEquals("EXPIRING", summary.validityStatus());
        assertEquals("FIT_WITH_RESTRICTIONS", summary.fitnessResult());
    }

    // ─── Protecção de dados (CL-01, CL-02, CL-03) ─────────────────────────────

    /**
     * O resumo continua a ser dado de saúde. Antes desta guarda, qualquer conta autenticada da
     * empresa lia o estado de aptidão de qualquer colega trocando o número no endereço.
     */
    @Test
    void summaryOfAnotherEmployeeIsBlockedForNonManager() {
        Employee colleague = new Employee();
        colleague.setId(9L);
        colleague.setName("Bruno Chissano");
        when(employeeRepository.findByIdAndCompanyId(9L, 7L)).thenReturn(Optional.of(colleague));
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "trabalhador"))
                .thenReturn(Optional.of(employee));
        CurrentUserContext.setCurrentUser("trabalhador", "EMPLOYEE");

        BusinessRuleException error =
                assertThrows(BusinessRuleException.class, () -> service.summary(9L));

        assertTrue(error.getMessage().contains("dado de saúde"));
        verify(repository, never())
                .findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(anyLong(), anyLong());
    }

    /** O próprio continua a ver a sua aptidão — senão o self-service morria com a guarda. */
    @Test
    void summaryOfSelfIsAllowedForEmployee() {
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "trabalhador"))
                .thenReturn(Optional.of(employee));
        when(repository.findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(7L, 3L))
                .thenReturn(Optional.of(exam("FIT", LocalDate.now().plusMonths(6))));
        CurrentUserContext.setCurrentUser("trabalhador", "EMPLOYEE");

        assertEquals("VALID", service.summary(3L).validityStatus());
    }

    /** Ler dados de saúde deixa rasto — é o registo de acesso que a protecção de dados exige. */
    @Test
    void readingClinicalHistoryIsAudited() {
        when(repository.findHistory(7L, 3L)).thenReturn(List.of(exam("FIT", LocalDate.now().plusMonths(6))));

        service.history(3L);

        verify(auditLogService).logCurrent(eq("OCCUPATIONAL_HEALTH_ACCESS"), contains("Ana Matola"));
    }

    /**
     * A Lei n.º 19/2014 e a Lei n.º 13/2023 proíbem apurar o estado serológico do trabalhador. O
     * campo de observações sem regra era exactamente onde isso ia parar.
     */
    @Test
    void serologyInFreeTextIsRejected() {
        var invalid = new SaveOccupationalHealthExamRequest(3L, null, LocalDate.now(),
                LocalDate.now().plusYears(1), "FIT", null, "Clínica Central", "Dra. Langa",
                null, "Teste de HIV negativo", null, null, null, null);

        BusinessRuleException error =
                assertThrows(BusinessRuleException.class, () -> service.register(invalid));

        assertTrue(error.getMessage().contains("serológico"));
        verify(repository, never()).save(any());
    }

    /** Uma restrição legítima com palavras normais não pode ser apanhada pela guarda. */
    @Test
    void ordinaryRestrictionIsNotMistakenForProhibitedData() {
        var request = new SaveOccupationalHealthExamRequest(3L, null, LocalDate.now(),
                LocalDate.now().plusYears(1), "FIT_WITH_RESTRICTIONS", null, "Clínica Central", null,
                "Sem levantamento de cargas acima de 20 kg; assiduidade sem restrição", null,
                null, null, null, null);

        assertEquals("FIT_WITH_RESTRICTIONS", service.register(request).fitnessResult());
    }

    // ─── Prestador e custo (CL-10..CL-14) ─────────────────────────────────────

    @Test
    void registerLinksProviderAndCost() {
        when(supplierRepository.findById(4L)).thenReturn(Optional.of(supplier(4L, "Clínica Sommerschield", true)));

        var result = service.register(new SaveOccupationalHealthExamRequest(3L, null, LocalDate.now(),
                LocalDate.now().plusYears(1), "FIT", 4L, null, "Dra. Langa", null, null,
                new BigDecimal("2500.00"), "FT/2026/88", null, null));

        assertEquals(4L, result.providerId());
        assertEquals("Clínica Sommerschield", result.providerName());
        assertEquals(0, new BigDecimal("2500.00").compareTo(result.cost()));
        assertFalse(result.paid());
    }

    @Test
    void providerFromAnotherCompanyIsRejected() {
        Company other = new Company();
        other.setId(99L);
        Supplier foreign = supplier(4L, "Clínica de outra empresa", true);
        foreign.setCompany(other);
        when(supplierRepository.findById(4L)).thenReturn(Optional.of(foreign));

        assertThrows(BusinessRuleException.class, () -> service.register(
                new SaveOccupationalHealthExamRequest(3L, null, LocalDate.now(),
                        LocalDate.now().plusYears(1), "FIT", 4L, null, null, null, null,
                        null, null, null, null)));
    }

    /** Um número de factura sem valor é uma despesa que nunca chega a existir. */
    @Test
    void invoiceWithoutCostIsRejected() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.register(
                new SaveOccupationalHealthExamRequest(3L, null, LocalDate.now(),
                        LocalDate.now().plusYears(1), "FIT", null, "Clínica Central", null, null, null,
                        null, "FT/2026/88", null, null)));
        assertTrue(error.getMessage().contains("valor"));
    }

    @Test
    void payingExamLeavesTreasuryAndIsAudited() {
        OccupationalHealthExam exam = exam("FIT", LocalDate.now().plusYears(1));
        exam.setId(10L);
        exam.setCost(new BigDecimal("2500.00"));
        exam.setClinic("Clínica Central");
        when(repository.findByIdAndCompanyId(10L, 7L)).thenReturn(Optional.of(exam));

        var result = service.payExam(10L);

        assertTrue(result.paid());
        assertEquals(LocalDate.now(), result.paidAt());
        verify(financeService).registerAutoPayout(eq(new BigDecimal("2500.00")), contains("Clínica Central"));
        verify(auditLogService).logCurrent(eq("OCCUPATIONAL_HEALTH_EXAM_PAID"), anyString());
    }

    @Test
    void payingTheSameExamTwiceIsRejected() {
        OccupationalHealthExam exam = exam("FIT", LocalDate.now().plusYears(1));
        exam.setId(10L);
        exam.setCost(new BigDecimal("2500.00"));
        exam.setPaidAt(LocalDate.now().minusDays(3));
        when(repository.findByIdAndCompanyId(10L, 7L)).thenReturn(Optional.of(exam));

        assertThrows(BusinessRuleException.class, () -> service.payExam(10L));
        verify(financeService, never()).registerAutoPayout(any(), anyString());
    }

    @Test
    void payingExamWithoutCostIsRejected() {
        OccupationalHealthExam exam = exam("FIT", LocalDate.now().plusYears(1));
        exam.setId(10L);
        when(repository.findByIdAndCompanyId(10L, 7L)).thenReturn(Optional.of(exam));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.payExam(10L));
        assertTrue(error.getMessage().contains("custo"));
        verify(financeService, never()).registerAutoPayout(any(), anyString());
    }

    @Test
    void costReportAggregatesByProvider() {
        OccupationalHealthExam first = exam("FIT", LocalDate.now().plusYears(1));
        first.setProvider(supplier(4L, "Clínica Sommerschield", true));
        first.setCost(new BigDecimal("2500.00"));
        first.setPaidAt(LocalDate.now().minusDays(1));
        OccupationalHealthExam second = exam("FIT", LocalDate.now().plusYears(1));
        second.setProvider(supplier(4L, "Clínica Sommerschield", true));
        second.setCost(new BigDecimal("1500.00"));
        OccupationalHealthExam free = exam("FIT", LocalDate.now().plusYears(1));
        when(repository.findByPeriod(eq(7L), any(), any())).thenReturn(List.of(first, second, free));

        var report = service.costReport(LocalDate.now().withDayOfYear(1), LocalDate.now());

        assertEquals(2, report.examCount());
        assertEquals(0, new BigDecimal("4000.00").compareTo(report.total()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(report.pending()));
        assertEquals(1, report.byProvider().size());
        assertEquals("Clínica Sommerschield", report.byProvider().get(0).providerName());
    }

    /**
     * Quem nunca fez exame nunca tem validade a caducar, logo nunca aparecia em aviso nenhum — e é
     * o caso que a inspecção do trabalho encontra primeiro.
     */
    @Test
    void missingExamsListsActiveEmployeesWithoutAnyExam() {
        Employee withExam = new Employee();
        withExam.setId(3L);
        withExam.setName("Ana Matola");
        withExam.setHireDate(LocalDate.now().minusYears(2));
        Employee without = new Employee();
        without.setId(9L);
        without.setName("Bruno Chissano");
        without.setHireDate(LocalDate.now().minusDays(200));
        Employee terminated = new Employee();
        terminated.setId(11L);
        terminated.setName("Carla Tembe");
        terminated.setStatus("TERMINATED");
        when(employeeRepository.findByCompanyIdOrderByName(7L))
                .thenReturn(List.of(withExam, without, terminated));
        when(repository.findEmployeeIdsWithExam(7L)).thenReturn(List.of(3L));

        var missing = service.missingExams();

        assertEquals(1, missing.size());
        assertEquals("Bruno Chissano", missing.get(0).employeeName());
        assertEquals(200L, missing.get(0).daysSinceHire());
    }

    // ─── Auxiliares ───────────────────────────────────────────────────────────

    private OccupationalHealthExam exam(String result, LocalDate expiry) {
        OccupationalHealthExam exam = new OccupationalHealthExam();
        exam.setCompany(company);
        exam.setEmployee(employee);
        exam.setExamDate(LocalDate.now().minusMonths(2));
        exam.setExpiryDate(expiry);
        exam.setFitnessResult(result);
        return exam;
    }

    private Supplier supplier(Long id, String name, boolean active) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        supplier.setTaxId("400000000");
        supplier.setActive(active);
        supplier.setCompany(company);
        return supplier;
    }

    private SaveOccupationalHealthExamRequest request(String result, String restrictions, byte[] attachment) {
        return new SaveOccupationalHealthExamRequest(3L, "CS-3", LocalDate.now(),
                LocalDate.now().plusYears(1), result, null, "Clínica Central", "Dra. Langa",
                restrictions, null, null, null, attachment == null ? null : "exame.pdf", attachment);
    }
}

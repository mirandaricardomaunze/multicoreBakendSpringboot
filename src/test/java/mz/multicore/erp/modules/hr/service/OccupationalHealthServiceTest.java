package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.SaveOccupationalHealthExamRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OccupationalHealthServiceTest {
    private OccupationalHealthExamRepository repository;
    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private AuditLogService auditLogService;
    private OccupationalHealthService service;

    @BeforeEach
    void setUp() {
        repository = mock(OccupationalHealthExamRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new OccupationalHealthService(repository, employeeRepository, companyRepository, auditLogService);
        CurrentUserContext.setCurrentCompanyId(7L);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        Company company = new Company();
        company.setId(7L);
        Employee employee = new Employee();
        employee.setId(3L);
        employee.setName("Ana Matola");
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        when(employeeRepository.findByIdAndCompanyId(3L, 7L)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(invocation -> {
            OccupationalHealthExam exam = invocation.getArgument(0);
            exam.setId(10L);
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
                LocalDate.now().minusDays(1), "FIT", null, null, null, null, null, null);
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
        OccupationalHealthExam exam = new OccupationalHealthExam();
        Employee employee = employeeRepository.findByIdAndCompanyId(3L, 7L).orElseThrow();
        exam.setEmployee(employee);
        exam.setExamDate(LocalDate.now().minusMonths(2));
        exam.setExpiryDate(LocalDate.now().plusDays(30));
        exam.setFitnessResult("FIT_WITH_RESTRICTIONS");
        exam.setRestrictions("Não levantar cargas");
        when(repository.findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(7L, 3L))
                .thenReturn(Optional.of(exam));

        var summary = service.summary(3L);

        assertEquals("EXPIRING", summary.validityStatus());
        assertEquals("FIT_WITH_RESTRICTIONS", summary.fitnessResult());
    }

    private SaveOccupationalHealthExamRequest request(String result, String restrictions, byte[] attachment) {
        return new SaveOccupationalHealthExamRequest(3L, "CS-3", LocalDate.now(),
                LocalDate.now().plusYears(1), result, "Clínica Central", "Dra. Langa",
                restrictions, null, attachment == null ? null : "exame.pdf", attachment);
    }
}

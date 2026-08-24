package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.SaveEmployeeDocumentRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmployeeDocument;
import mz.multicore.erp.modules.hr.repository.EmployeeDocumentRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** B8.8 do harness do RH (RHC-76): o DIRE caducar sem aviso é multa. */
class EmployeeDocumentServiceTest {

    private static final Long COMPANY = 7L;

    private EmployeeDocumentRepository documentRepository;
    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private AuditLogService auditLogService;
    private EmployeeDocumentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(EmployeeDocumentRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new EmployeeDocumentService(documentRepository, employeeRepository,
                companyRepository, auditLogService);

        Company company = new Company();
        company.setId(COMPANY);
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company));
        when(employeeRepository.findByIdAndCompanyId(5L, COMPANY)).thenReturn(Optional.of(employee()));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void savesADocumentWithItsExpiry_andAudits() { // RHC-76
        var dto = service.save(new SaveEmployeeDocumentRequest(5L, "dire", "DIRE-99",
                LocalDate.of(2024, 1, 10), LocalDate.now().plusDays(20), null));

        assertEquals("DIRE", dto.documentType());
        assertEquals(20L, dto.daysUntilExpiry());
        assertFalse(dto.expired());
        verify(auditLogService).logCurrent(eq("EMPLOYEE_DOCUMENT_SAVE"), any());
    }

    @Test
    void aDocumentWithoutExpiry_doesNotExpire() {
        // Nulo significa "não caduca" (NUIT, BI vitalício), e não "ainda não preenchi" — avisar
        // sobre ele seria ruído que ensina a ignorar o sino.
        var dto = service.save(new SaveEmployeeDocumentRequest(5L, "NUIT", "123456789",
                null, null, null));

        assertNull(dto.daysUntilExpiry());
        assertFalse(dto.expired());
    }

    @Test
    void expiryBeforeIssue_isRefused() {
        var ex = assertThrows(BusinessRuleException.class, () -> service.save(
                new SaveEmployeeDocumentRequest(5L, "DIRE", "X", LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 4, 1), null)));

        assertTrue(ex.getMessage().contains("anterior à emissão"));
    }

    @Test
    void alreadyExpiredDocuments_stayInTheAlertList() { // RHC-76
        // Sair da janela não pode ser a forma de o alerta desaparecer: um DIRE vencido há duas
        // semanas é mais urgente do que um que vence daqui a cinco dias.
        when(documentRepository.findExpiringUntil(eq(COMPANY), any()))
                .thenReturn(List.of(document(LocalDate.now().minusDays(14))));

        var alerts = service.expiringSoon();

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).expired());
        assertEquals(-14L, alerts.get(0).daysUntilExpiry());
    }

    @Test
    void employeeRole_cannotSaveDocuments() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.save(
                new SaveEmployeeDocumentRequest(5L, "DIRE", "X", null, null, null)));
        verify(documentRepository, never()).save(any());
    }

    private EmployeeDocument document(LocalDate expiry) {
        Company company = new Company();
        company.setId(COMPANY);
        EmployeeDocument document = new EmployeeDocument();
        document.setId(1L);
        document.setCompany(company);
        document.setEmployee(employee());
        document.setDocumentType("DIRE");
        document.setExpiryDate(expiry);
        return document;
    }

    private static Employee employee() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-5");
        employee.setName("Maria");
        return employee;
    }
}

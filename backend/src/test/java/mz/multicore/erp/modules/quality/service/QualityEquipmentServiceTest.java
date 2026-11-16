package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.service.EmployeeLookup;
import mz.multicore.erp.modules.quality.dto.*;
import mz.multicore.erp.modules.quality.model.Equipment;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.EquipmentRepository;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QualityEquipmentServiceTest {

    private EquipmentRepository equipmentRepository;
    private WorkerEquipmentAssignmentRepository assignmentRepository;
    private EmployeeRepository employeeRepository;
    private CompanyService companyService;
    private QualityEquipmentService service;

    private Company company;
    private Employee employee;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        CurrentUserContext.clear();
        equipmentRepository = mock(EquipmentRepository.class);
        assignmentRepository = mock(WorkerEquipmentAssignmentRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyService = mock(CompanyService.class);

        service = new QualityEquipmentService(
                equipmentRepository, assignmentRepository,
                new EmployeeLookup(employeeRepository, mock(PayslipRepository.class),
                        mock(EmploymentContractRepository.class)),
                companyService
        );

        company = new Company();
        company.setId(1L);
        company.setName("Empresa Moçambique Teste");

        employee = new Employee();
        employee.setId(10L);
        employee.setCompany(company);
        employee.setName("Armando Sitoe");
        employee.setDepartment("Manutenção");

        equipment = new Equipment();
        equipment.setId(20L);
        equipment.setCompany(company);
        equipment.setCode("EPI-BOT-01");
        equipment.setName("Botas com Biqueira de Aço");
        equipment.setCategory(EquipmentCategory.PPE_PROTECTION);
        equipment.setPeriodValue(6);
        equipment.setPeriodUnit(PeriodUnit.MONTHS);
        equipment.setWarningDaysBefore(15);
        equipment.setActive(true);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("QST-01: Cadastro de equipamento valida obrigatoriedade de campos e prazo > 0")
    void testCreateEquipmentValidations() {
        assertThrows(BusinessRuleException.class, () ->
                service.createEquipment(new EquipmentCreateRequest(null, "EPI-1", "Bota", EquipmentCategory.PPE_PROTECTION, 6, PeriodUnit.MONTHS, "")));

        assertThrows(BusinessRuleException.class, () ->
                service.createEquipment(new EquipmentCreateRequest(1L, "EPI-1", "", EquipmentCategory.PPE_PROTECTION, 6, PeriodUnit.MONTHS, "")));

        assertThrows(BusinessRuleException.class, () ->
                service.createEquipment(new EquipmentCreateRequest(1L, "EPI-1", "Bota", EquipmentCategory.PPE_PROTECTION, 0, PeriodUnit.MONTHS, "")));
    }

    @Test
    @DisplayName("QST-02: Cadastro de equipamento impede código duplicado na mesma empresa")
    void testCreateEquipmentDuplicateCode() {
        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(equipmentRepository.existsByCompanyIdAndCodeIgnoreCase(1L, "EPI-BOT-01")).thenReturn(true);

        EquipmentCreateRequest req = new EquipmentCreateRequest(
                1L, "EPI-BOT-01", "Outra Bota", EquipmentCategory.PPE_PROTECTION, 6, PeriodUnit.MONTHS, ""
        );
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.createEquipment(req));
        assertTrue(ex.getMessage().contains("Já existe um equipamento"));
    }

    @Test
    @DisplayName("QST-03: Atribuição calcula automaticamente a data de expiração com base no prazo do equipamento")
    void testAssignEquipmentCalculatesExpiryDate() {
        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(equipmentRepository.findByIdAndCompanyId(20L, 1L)).thenReturn(Optional.of(equipment));
        when(employeeRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(employee));
        when(assignmentRepository.save(any(WorkerEquipmentAssignment.class))).thenAnswer(invocation -> {
            WorkerEquipmentAssignment a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        LocalDate delivery = LocalDate.of(2026, 1, 15);
        EquipmentAssignmentRequest req = new EquipmentAssignmentRequest(
                1L, 20L, 10L, delivery, 1, "42", "SN-999", "Entrega inicial"
        );

        WorkerEquipmentAssignmentDTO dto = service.assignEquipment(req);

        assertNotNull(dto);
        assertEquals(LocalDate.of(2026, 7, 15), dto.expiryDate(), "Data de expiração deve ser 6 meses após a entrega");
        assertEquals("Armando Sitoe", dto.employeeName());
        assertEquals("Botas com Biqueira de Aço", dto.equipmentName());
    }

    @Test
    @DisplayName("QST-04: Renovação encerra a atribuição anterior e gera novo registo com prazo renovado")
    void testRenewAssignment() {
        WorkerEquipmentAssignment current = new WorkerEquipmentAssignment();
        current.setId(100L);
        current.setCompany(company);
        current.setEquipment(equipment);
        current.setEmployee(employee);
        current.setAssignedDate(LocalDate.of(2025, 6, 1));
        current.setExpiryDate(LocalDate.of(2025, 12, 1));
        current.setQuantity(1);
        current.setSizeOrSpec("42");
        current.setStatus(AssignmentStatus.EXPIRED);

        when(assignmentRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(current));
        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(equipmentRepository.findByIdAndCompanyId(20L, 1L)).thenReturn(Optional.of(equipment));
        when(employeeRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(employee));
        when(assignmentRepository.save(any(WorkerEquipmentAssignment.class))).thenAnswer(inv -> {
            WorkerEquipmentAssignment a = inv.getArgument(0);
            if (a.getId() == null) a.setId(200L);
            return a;
        });

        LocalDate renewalDate = LocalDate.of(2026, 2, 1);
        WorkerEquipmentAssignmentDTO renewed = service.renewAssignment(100L, 1L, renewalDate, "Renovação de botas");

        assertEquals(AssignmentStatus.RENEWED, current.getStatus(), "Registo anterior deve ficar marcado como RENEWED");
        assertNotNull(renewed);
        assertEquals(LocalDate.of(2026, 8, 1), renewed.expiryDate(), "Novo registo deve ter 6 meses calculados a partir da renovação");
    }

    @Test
    @DisplayName("QST-05: Radar agrupa automaticamente colaboradores com equipamentos expirados")
    void testGetExpiredWorkerAlerts() {
        LocalDate today = LocalDate.of(2026, 5, 1);

        WorkerEquipmentAssignment expired = new WorkerEquipmentAssignment();
        expired.setId(101L);
        expired.setCompany(company);
        expired.setEmployee(employee);
        expired.setEquipment(equipment);
        expired.setAssignedDate(LocalDate.of(2025, 1, 1));
        expired.setExpiryDate(LocalDate.of(2025, 7, 1));
        expired.setStatus(AssignmentStatus.ACTIVE);

        when(assignmentRepository.findExpiredActiveAssignments(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(expired));
        when(assignmentRepository.findExpiringSoonAssignments(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        List<ExpiredWorkerAlertDTO> alerts = service.getExpiredWorkerAlerts(1L);

        assertEquals(1, alerts.size(), "Deve conter 1 colaborador com itens expirados");
        ExpiredWorkerAlertDTO alert = alerts.get(0);
        assertEquals("Armando Sitoe", alert.employeeName());
        assertEquals(1, alert.totalExpiredItems());
        assertEquals(1, alert.items().size());
        assertEquals(AssignmentStatus.EXPIRED, alert.items().get(0).status());
    }

    @Test
    @DisplayName("QST-06: Pesquisa paginada retorna PageResponse no padrão canónico")
    void testSearchAssignmentsPagination() {
        WorkerEquipmentAssignment a = new WorkerEquipmentAssignment();
        a.setId(1L);
        a.setCompany(company);
        a.setEmployee(employee);
        a.setEquipment(equipment);
        a.setAssignedDate(LocalDate.now());
        a.setExpiryDate(LocalDate.now().plusMonths(6));

        PageImpl<WorkerEquipmentAssignment> page = new PageImpl<>(List.of(a), Pageable.ofSize(25), 1);
        when(assignmentRepository.searchAssignments(eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<WorkerEquipmentAssignmentDTO> resp = service.searchAssignments(1L, null, null, null, 0, 25);

        assertNotNull(resp);
        assertEquals(1, resp.totalElements());
        assertEquals(1, resp.items().size());
    }

    @Test
    @DisplayName("QST-07: empresa do pedido nunca pode divergir do tenant autenticado")
    void rejectsCrossTenantCompanyId() {
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(1L);

        EquipmentCreateRequest request = new EquipmentCreateRequest(
                2L, "EPI-2", "Capacete", EquipmentCategory.PPE_PROTECTION,
                6, PeriodUnit.MONTHS, "Protecção");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.createEquipment(request));

        assertTrue(ex.getMessage().contains("outra empresa"));
        verifyNoInteractions(companyService, equipmentRepository);
    }

    @Test
    @DisplayName("QST-08: quantidade inválida é rejeitada sem normalização silenciosa")
    void rejectsNonPositiveAssignmentQuantity() {
        EquipmentAssignmentRequest request = new EquipmentAssignmentRequest(
                1L, 20L, 10L, LocalDate.now(), 0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.assignEquipment(request));

        assertTrue(ex.getMessage().contains("quantidade"));
        verifyNoInteractions(companyService, equipmentRepository, employeeRepository, assignmentRepository);
    }
}

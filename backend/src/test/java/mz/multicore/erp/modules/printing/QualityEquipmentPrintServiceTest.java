package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.quality.dto.AssignmentStatus;
import mz.multicore.erp.modules.quality.dto.EquipmentCategory;
import mz.multicore.erp.modules.quality.dto.PeriodUnit;
import mz.multicore.erp.modules.quality.model.Equipment;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QualityEquipmentPrintServiceTest {

    private WorkerEquipmentAssignmentRepository assignmentRepository;
    private QualityEquipmentPrintService service;

    @BeforeEach
    void setup() {
        assignmentRepository = mock(WorkerEquipmentAssignmentRepository.class);
        service = new QualityEquipmentPrintService(assignmentRepository);
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("QST-PRINT-01: Gera PDF válido do Termo de Entrega e Responsabilidade de EPI")
    void testRenderDeliverySheetSuccess() throws IOException {
        Company company = new Company();
        company.setId(1L);
        company.setName("Multicore Soluções Lda");
        company.setTaxId("400998877");
        company.setAddress("Av. 24 de Julho, Maputo");

        Employee employee = new Employee();
        employee.setId(10L);
        employee.setName("Alberto Nhaca");
        employee.setEmployeeNumber("EMP-0010");
        employee.setDepartment("Operações & Armazém");
        employee.setTaxId("102938475");

        Equipment equipment = new Equipment();
        equipment.setId(20L);
        equipment.setCode("EPI-BOT-01");
        equipment.setName("Bota de Proteção Biqueira de Aço");
        equipment.setCategory(EquipmentCategory.PPE_PROTECTION);
        equipment.setPeriodValue(6);
        equipment.setPeriodUnit(PeriodUnit.MONTHS);
        equipment.setWarningDaysBefore(15);

        WorkerEquipmentAssignment assignment = new WorkerEquipmentAssignment();
        assignment.setId(101L);
        assignment.setCompany(company);
        assignment.setEmployee(employee);
        assignment.setEquipment(equipment);
        assignment.setAssignedDate(LocalDate.now());
        assignment.setExpiryDate(LocalDate.now().plusMonths(6));
        assignment.setQuantity(1);
        assignment.setSizeOrSpec("Tamanho 42");
        assignment.setSerialNumber("SN-BOT-42-009");
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setNotes("Entrega de EPI novo em conformidade com normas HST.");

        when(assignmentRepository.findByIdWithDetailsAndCompanyId(101L, 1L))
                .thenReturn(Optional.of(assignment));

        byte[] pdf = service.renderDeliverySheet(101L, 1L);
        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "O PDF gerado deve conter conteúdo substancial");

        PdfReader reader = new PdfReader(pdf);
        assertTrue(reader.getNumberOfPages() >= 1, "O PDF deve ter pelo menos 1 página");
        reader.close();
    }

    @Test
    @DisplayName("QST-PRINT-02: Lança BusinessRuleException quando a atribuição não existe")
    void testRenderDeliverySheetNotFound() {
        when(assignmentRepository.findByIdWithDetailsAndCompanyId(999L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.renderDeliverySheet(999L, 1L));
    }
}

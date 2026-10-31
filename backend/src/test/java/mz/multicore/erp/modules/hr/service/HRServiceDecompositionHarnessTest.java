package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HRServiceDecompositionHarnessTest {

    @Test
    void hrServiceDelegatesToCohesiveCollaboratorsAndRespectsLineLimits() throws Exception {
        HrEmployeeManagementService empService = mock(HrEmployeeManagementService.class);
        HrPayslipIssuanceService issuanceService = mock(HrPayslipIssuanceService.class);
        HrPayslipLifecycleService lifecycleService = mock(HrPayslipLifecycleService.class);
        HrAbsenceAndVacationService absenceVacationService = mock(HrAbsenceAndVacationService.class);

        HRService service = new HRService(empService, issuanceService, lifecycleService, absenceVacationService);

        when(empService.getAllEmployees()).thenReturn(List.of());
        assertSame(List.of(), service.getAllEmployees());
        verify(empService).getAllEmployees();

        when(lifecycleService.getAllPayslips()).thenReturn(List.of());
        assertSame(List.of(), service.getAllPayslips());
        verify(lifecycleService).getAllPayslips();

        when(absenceVacationService.getAllAbsences()).thenReturn(List.of());
        assertSame(List.of(), service.getAllAbsences());
        verify(absenceVacationService).getAllAbsences();

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/hr/service");
        int facadeLines = Files.readAllLines(base.resolve("HRService.java")).size();
        int empLines = Files.readAllLines(base.resolve("HrEmployeeManagementService.java")).size();
        int issuanceLines = Files.readAllLines(base.resolve("HrPayslipIssuanceService.java")).size();
        int lifecycleLines = Files.readAllLines(base.resolve("HrPayslipLifecycleService.java")).size();
        int absVacLines = Files.readAllLines(base.resolve("HrAbsenceAndVacationService.java")).size();

        assertTrue(facadeLines <= 170, "HRService deve ter <= 170 linhas, mas tem " + facadeLines);
        assertTrue(empLines <= 230, "HrEmployeeManagementService deve ter <= 230 linhas, mas tem " + empLines);
        assertTrue(issuanceLines <= 290, "HrPayslipIssuanceService deve ter <= 290 linhas, mas tem " + issuanceLines);
        assertTrue(lifecycleLines <= 150, "HrPayslipLifecycleService deve ter <= 150 linhas, mas tem " + lifecycleLines);
        assertTrue(absVacLines <= 220, "HrAbsenceAndVacationService deve ter <= 220 linhas, mas tem " + absVacLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

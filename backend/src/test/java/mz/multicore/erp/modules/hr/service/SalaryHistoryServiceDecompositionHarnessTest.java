package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SalaryChangeReason;
import mz.multicore.erp.modules.hr.repository.SalaryChangeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalaryHistoryServiceDecompositionHarnessTest {

    @Test
    void salaryHistoryServiceDelegatesToMutationCollaboratorAndRespectsLineLimits() throws Exception {
        SalaryChangeRepository repo = mock(SalaryChangeRepository.class);
        SalaryChangeMutationService mutationService = mock(SalaryChangeMutationService.class);

        SalaryHistoryService service = new SalaryHistoryService(repo, mutationService);

        LocalDate today = LocalDate.of(2026, 10, 29);
        when(mutationService.salaryOn(5L, today)).thenReturn(Optional.of(new BigDecimal("25000.00")));
        assertEquals(Optional.of(new BigDecimal("25000.00")), service.salaryOn(5L, today));
        verify(mutationService).salaryOn(5L, today);

        Employee emp = new Employee();
        SalaryChangeDTO expected = mock(SalaryChangeDTO.class);
        SalaryChangeReason reason = SalaryChangeReason.values()[0];
        when(mutationService.record(emp, BigDecimal.TEN, today, reason, "Dev", "IT", null))
                .thenReturn(expected);
        assertSame(expected, service.record(emp, BigDecimal.TEN, today, reason, "Dev", "IT", null));

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/hr/service");
        int facadeLines = Files.readAllLines(base.resolve("SalaryHistoryService.java")).size();
        int mutationLines = Files.readAllLines(base.resolve("SalaryChangeMutationService.java")).size();

        assertTrue(facadeLines <= 75, "SalaryHistoryService deve ter <= 75 linhas, mas tem " + facadeLines);
        assertTrue(mutationLines <= 135, "SalaryChangeMutationService deve ter <= 135 linhas, mas tem " + mutationLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

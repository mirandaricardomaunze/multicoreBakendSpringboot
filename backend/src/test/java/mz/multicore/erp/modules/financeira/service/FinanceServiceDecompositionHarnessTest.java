package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceServiceDecompositionHarnessTest {

    @Test
    void financeServiceDelegatesToCollaboratorsAndRespectsLineLimits() throws Exception {
        TreasuryOperationService opService = mock(TreasuryOperationService.class);
        TreasuryQueryService queryService = mock(TreasuryQueryService.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);

        FinanceService service = new FinanceService(opService, queryService, invoiceRepository);

        when(queryService.getAllAccounts()).thenReturn(List.of());
        assertSame(List.of(), service.getAllAccounts());
        verify(queryService).getAllAccounts();

        service.registerAutoPayout(BigDecimal.TEN, "Salário");
        verify(opService).registerAutoPayout(BigDecimal.TEN, "Salário");

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/financeira/service");
        int facadeLines = Files.readAllLines(base.resolve("FinanceService.java")).size();
        int opLines = Files.readAllLines(base.resolve("TreasuryOperationService.java")).size();
        int queryLines = Files.readAllLines(base.resolve("TreasuryQueryService.java")).size();

        assertTrue(facadeLines <= 95, "FinanceService deve ter <= 95 linhas, mas tem " + facadeLines);
        assertTrue(opLines <= 100, "TreasuryOperationService deve ter <= 100 linhas, mas tem " + opLines);
        assertTrue(queryLines <= 70, "TreasuryQueryService deve ter <= 70 linhas, mas tem " + queryLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

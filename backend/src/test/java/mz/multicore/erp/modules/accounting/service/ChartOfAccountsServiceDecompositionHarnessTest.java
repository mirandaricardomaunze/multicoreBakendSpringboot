package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.SourceLines;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartOfAccountsServiceDecompositionHarnessTest {

    @Test
    void chartOfAccountsServiceDelegatesToCollaboratorsAndRespectsLineLimits() throws Exception {
        ChartOfAccountsMutationService mutationService = mock(ChartOfAccountsMutationService.class);
        ChartOfAccountsQueryService queryService = mock(ChartOfAccountsQueryService.class);

        ChartOfAccountsService service = new ChartOfAccountsService(mutationService, queryService);

        when(queryService.listAccounts()).thenReturn(List.of());
        assertSame(List.of(), service.listAccounts());
        verify(queryService).listAccounts();

        when(queryService.hasChart(1L)).thenReturn(true);
        assertTrue(service.hasChart(1L));

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/accounting/service");
        int facadeLines = SourceLines.code(base.resolve("ChartOfAccountsService.java"));
        int mutationLines = SourceLines.code(base.resolve("ChartOfAccountsMutationService.java"));
        int queryLines = SourceLines.code(base.resolve("ChartOfAccountsQueryService.java"));

        assertTrue(facadeLines <= 85, "ChartOfAccountsService deve ter <= 85 linhas, mas tem " + facadeLines);
        assertTrue(mutationLines <= 85, "ChartOfAccountsMutationService deve ter <= 85 linhas, mas tem " + mutationLines);
        assertTrue(queryLines <= 65, "ChartOfAccountsQueryService deve ter <= 65 linhas, mas tem " + queryLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

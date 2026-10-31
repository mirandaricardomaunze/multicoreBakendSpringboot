package mz.multicore.erp.modules.monitoring.service;

import mz.multicore.erp.modules.monitoring.dto.SystemAlertTestResultDTO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemAlertEmailServiceDecompositionHarnessTest {

    @Test
    void systemAlertEmailServiceDelegatesToDispatcherAndRespectsLineLimits() throws Exception {
        SystemAlertMailDispatcher dispatcher = mock(SystemAlertMailDispatcher.class);
        SystemAlertEmailService service = new SystemAlertEmailService(dispatcher);

        when(dispatcher.sendCriticalAlert("DB", "ACME", "Timeout", "ops@acme.co.mz")).thenReturn(true);
        assertTrue(service.sendCriticalAlert("DB", "ACME", "Timeout", "ops@acme.co.mz"));
        verify(dispatcher).sendCriticalAlert("DB", "ACME", "Timeout", "ops@acme.co.mz");

        SystemAlertTestResultDTO result = new SystemAlertTestResultDTO("EMAIL", true, "OK", Instant.now());
        when(dispatcher.testEmailAlert("ops@acme.co.mz")).thenReturn(result);
        assertSame(result, service.testEmailAlert("ops@acme.co.mz"));

        service.clearCooldowns();
        verify(dispatcher).clearCooldowns();

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/monitoring/service");
        int facadeLines = Files.readAllLines(base.resolve("SystemAlertEmailService.java")).size();
        int dispatcherLines = Files.readAllLines(base.resolve("SystemAlertMailDispatcher.java")).size();

        assertTrue(facadeLines <= 55, "SystemAlertEmailService deve ter <= 55 linhas, mas tem " + facadeLines);
        assertTrue(dispatcherLines <= 150, "SystemAlertMailDispatcher deve ter <= 150 linhas, mas tem " + dispatcherLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

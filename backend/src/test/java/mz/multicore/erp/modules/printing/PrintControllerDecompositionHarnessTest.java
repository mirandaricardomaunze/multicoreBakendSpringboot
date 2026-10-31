package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintControllerDecompositionHarnessTest {

    @Test
    @SuppressWarnings("unchecked")
    void printControllerDelegatesToDispatcherAndRespectsLineLimits() throws Exception {
        PrintRequestDispatcher dispatcher = mock(PrintRequestDispatcher.class);
        PrintController controller = new PrintController(dispatcher);

        ResponseEntity<Resource> expected = mock(ResponseEntity.class);
        when(dispatcher.invoice(42L)).thenReturn(expected);
        assertSame(expected, controller.invoice(42L));
        verify(dispatcher).invoice(42L);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/printing");
        int controllerLines = Files.readAllLines(base.resolve("PrintController.java")).size();
        int dispatcherLines = Files.readAllLines(base.resolve("PrintRequestDispatcher.java")).size();

        assertTrue(controllerLines <= 230, "PrintController deve ter <= 230 linhas, mas tem " + controllerLines);
        assertTrue(dispatcherLines <= 275, "PrintRequestDispatcher deve ter <= 275 linhas, mas tem " + dispatcherLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

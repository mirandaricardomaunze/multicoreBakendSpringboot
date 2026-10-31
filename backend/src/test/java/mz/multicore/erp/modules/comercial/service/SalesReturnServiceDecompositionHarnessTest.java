package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.ReturnableInvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardResultDTO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesReturnServiceDecompositionHarnessTest {

    @Test
    void salesReturnServiceDelegatesToCollaboratorsAndRespectsLineLimits() throws Exception {
        ReturnableInvoiceLineCalculator calculator = mock(ReturnableInvoiceLineCalculator.class);
        SalesReturnWizardProcessor processor = mock(SalesReturnWizardProcessor.class);

        SalesReturnService service = new SalesReturnService(calculator, processor);

        List<ReturnableInvoiceLineDTO> lines = List.of();
        when(calculator.getReturnableLines(9L)).thenReturn(lines);
        assertSame(lines, service.getReturnableLines(9L));
        verify(calculator).getReturnableLines(9L);

        SalesReturnWizardRequest req = mock(SalesReturnWizardRequest.class);
        SalesReturnWizardResultDTO res = mock(SalesReturnWizardResultDTO.class);
        when(processor.processReturn(req)).thenReturn(res);
        assertSame(res, service.processReturn(req));
        verify(processor).processReturn(req);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/comercial/service");
        int facadeLines = Files.readAllLines(base.resolve("SalesReturnService.java")).size();
        int calcLines = Files.readAllLines(base.resolve("ReturnableInvoiceLineCalculator.java")).size();
        int procLines = Files.readAllLines(base.resolve("SalesReturnWizardProcessor.java")).size();

        assertTrue(facadeLines <= 55, "SalesReturnService deve ter <= 55 linhas, mas tem " + facadeLines);
        assertTrue(calcLines <= 75, "ReturnableInvoiceLineCalculator deve ter <= 75 linhas, mas tem " + calcLines);
        assertTrue(procLines <= 120, "SalesReturnWizardProcessor deve ter <= 120 linhas, mas tem " + procLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

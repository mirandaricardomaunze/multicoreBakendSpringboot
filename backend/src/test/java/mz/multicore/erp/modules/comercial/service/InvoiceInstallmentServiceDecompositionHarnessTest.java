package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
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

class InvoiceInstallmentServiceDecompositionHarnessTest {

    @Test
    void invoiceInstallmentServiceDelegatesToCollaboratorsAndRespectsLineLimits() throws Exception {
        InstallmentPlanGenerator generator = mock(InstallmentPlanGenerator.class);
        InstallmentPaymentAmortizer amortizer = mock(InstallmentPaymentAmortizer.class);

        InvoiceInstallmentService service = new InvoiceInstallmentService(generator, amortizer);

        CreateInstallmentPlanRequest req = mock(CreateInstallmentPlanRequest.class);
        List<InvoiceInstallmentDTO> expected = List.of();

        when(generator.createInstallmentPlan(1L, req)).thenReturn(expected);
        assertSame(expected, service.createInstallmentPlan(1L, req));
        verify(generator).createInstallmentPlan(1L, req);

        when(amortizer.applyPaymentToInvoiceInstallments(1L, 10L, BigDecimal.TEN)).thenReturn(expected);
        assertSame(expected, service.applyPaymentToInvoiceInstallments(1L, 10L, BigDecimal.TEN));
        verify(amortizer).applyPaymentToInvoiceInstallments(1L, 10L, BigDecimal.TEN);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/comercial/service");
        int facadeLines = Files.readAllLines(base.resolve("InvoiceInstallmentService.java")).size();
        int generatorLines = Files.readAllLines(base.resolve("InstallmentPlanGenerator.java")).size();
        int amortizerLines = Files.readAllLines(base.resolve("InstallmentPaymentAmortizer.java")).size();

        assertTrue(facadeLines <= 65, "InvoiceInstallmentService deve ter <= 65 linhas, mas tem " + facadeLines);
        assertTrue(generatorLines <= 135, "InstallmentPlanGenerator deve ter <= 135 linhas, mas tem " + generatorLines);
        assertTrue(amortizerLines <= 65, "InstallmentPaymentAmortizer deve ter <= 65 linhas, mas tem " + amortizerLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

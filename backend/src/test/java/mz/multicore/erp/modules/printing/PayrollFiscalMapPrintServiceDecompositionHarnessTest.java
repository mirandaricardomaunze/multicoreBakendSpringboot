package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.hr.dto.PayrollFiscalSummaryDTO;
import mz.multicore.erp.modules.hr.service.PayrollTaxService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollFiscalMapPrintServiceDecompositionHarnessTest {

    @Test
    void payrollFiscalMapPrintServiceDelegatesToRendererAndRespectsLineLimits() throws Exception {
        PayrollTaxService taxService = mock(PayrollTaxService.class);
        CompanyService companyService = mock(CompanyService.class);
        PayrollFiscalMapDocumentRenderer renderer = mock(PayrollFiscalMapDocumentRenderer.class);

        PayrollFiscalMapPrintService service = new PayrollFiscalMapPrintService(taxService, companyService, renderer);

        Company company = new Company();
        PayrollFiscalSummaryDTO summary = mock(PayrollFiscalSummaryDTO.class);
        byte[] expected = new byte[]{1, 2, 3};

        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(taxService.fiscalSummary(2026, 10)).thenReturn(summary);
        when(renderer.render(company, summary, 2026, 10)).thenReturn(expected);

        assertSame(expected, service.render(1L, 2026, 10));
        verify(renderer).render(company, summary, 2026, 10);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/printing");
        int facadeLines = Files.readAllLines(base.resolve("PayrollFiscalMapPrintService.java")).size();
        int rendererLines = Files.readAllLines(base.resolve("PayrollFiscalMapDocumentRenderer.java")).size();

        assertTrue(facadeLines <= 55, "PayrollFiscalMapPrintService deve ter <= 55 linhas, mas tem " + facadeLines);
        assertTrue(rendererLines <= 150, "PayrollFiscalMapDocumentRenderer deve ter <= 150 linhas, mas tem " + rendererLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

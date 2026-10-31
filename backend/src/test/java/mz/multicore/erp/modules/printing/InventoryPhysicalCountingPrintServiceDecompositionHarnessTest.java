package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.InventorySessionDTO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryPhysicalCountingPrintServiceDecompositionHarnessTest {

    @Test
    void inventoryPhysicalCountingPrintServiceDelegatesToRendererAndRespectsLineLimits() throws Exception {
        CompanyService companyService = mock(CompanyService.class);
        InventoryPhysicalCountingDocumentRenderer renderer = mock(InventoryPhysicalCountingDocumentRenderer.class);

        InventoryPhysicalCountingPrintService service = new InventoryPhysicalCountingPrintService(companyService, renderer);

        Company company = new Company();
        InventorySessionDTO session = mock(InventorySessionDTO.class);
        byte[] expected = new byte[]{4, 5, 6};

        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(renderer.render(company, session)).thenReturn(expected);

        assertSame(expected, service.render(session, 1L));
        verify(renderer).render(company, session);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/printing");
        int facadeLines = Files.readAllLines(base.resolve("InventoryPhysicalCountingPrintService.java")).size();
        int rendererLines = Files.readAllLines(base.resolve("InventoryPhysicalCountingDocumentRenderer.java")).size();

        assertTrue(facadeLines <= 50, "InventoryPhysicalCountingPrintService deve ter <= 50 linhas, mas tem " + facadeLines);
        assertTrue(rendererLines <= 145, "InventoryPhysicalCountingDocumentRenderer deve ter <= 145 linhas, mas tem " + rendererLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

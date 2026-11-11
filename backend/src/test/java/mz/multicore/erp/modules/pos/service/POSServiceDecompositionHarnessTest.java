package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class POSServiceDecompositionHarnessTest {

    @Test
    void posServiceDelegatesToCohesiveCollaboratorsAndRespectsLineLimits() throws Exception {
        TillSessionRepository tillSessionRepo = mock(TillSessionRepository.class);
        CompanyLookup companyRepo = mock(CompanyLookup.class);
        FinanceService financeService = mock(FinanceService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        PosSessionAndShiftService sessionService = mock(PosSessionAndShiftService.class);
        PosZReportCalculator zReportCalculator = mock(PosZReportCalculator.class);
        PosCheckoutAndPaymentService checkoutService = mock(PosCheckoutAndPaymentService.class);
        PosReturnAndVoucherService returnService = mock(PosReturnAndVoucherService.class);

        POSService service = new POSService(
                tillSessionRepo, companyRepo, financeService, auditLogService,
                sessionService, zReportCalculator, checkoutService, returnService);

        when(sessionService.getSessionsByCompany(1L)).thenReturn(List.of());
        assertSame(List.of(), service.getSessionsByCompany(1L));
        verify(sessionService).getSessionsByCompany(1L);

        PosZReportDTO zReport = mock(PosZReportDTO.class);
        when(zReportCalculator.buildZReport(5L)).thenReturn(zReport);
        assertSame(zReport, service.buildZReport(5L));
        verify(zReportCalculator).buildZReport(5L);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/pos/service");
        int facadeLines = Files.readAllLines(base.resolve("POSService.java")).size();
        int sessionLines = Files.readAllLines(base.resolve("PosSessionAndShiftService.java")).size();
        int zReportLines = Files.readAllLines(base.resolve("PosZReportCalculator.java")).size();
        int checkoutLines = Files.readAllLines(base.resolve("PosCheckoutAndPaymentService.java")).size();
        int returnLines = Files.readAllLines(base.resolve("PosReturnAndVoucherService.java")).size();

        assertTrue(facadeLines <= 280, "POSService deve ter <= 280 linhas, mas tem " + facadeLines);
        assertTrue(sessionLines <= 225, "PosSessionAndShiftService deve ter <= 225 linhas, mas tem " + sessionLines);
        assertTrue(zReportLines <= 150, "PosZReportCalculator deve ter <= 150 linhas, mas tem " + zReportLines);
        assertTrue(checkoutLines <= 370, "PosCheckoutAndPaymentService deve ter <= 370 linhas, mas tem " + checkoutLines);
        assertTrue(returnLines <= 320, "PosReturnAndVoucherService deve ter <= 320 linhas, mas tem " + returnLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}

package mz.multicore.erp.gui.pos;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class PosZReportUiHarnessTest {

    private POSApiClient posApiClient;

    @BeforeEach
    void setUp() {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(), "Skipping UI Dialog test in headless environment");
        posApiClient = Mockito.mock(POSApiClient.class);
    }

    @Test
    void testPosBlindCloseDialogInstantiation() {
        TillSessionDTO session = new TillSessionDTO(
                1L, "operador", 1L, BigDecimal.valueOf(100),
                BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ZERO,
                LocalDateTime.now(), null, "OPEN"
        );
        PosZReportDTO z = new PosZReportDTO(
                1L, "operador", LocalDateTime.now(), LocalDateTime.now(), "CLOSED",
                BigDecimal.valueOf(100), BigDecimal.valueOf(500), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(500),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(600),
                BigDecimal.valueOf(600), BigDecimal.ZERO, 5, 0
        );

        when(posApiClient.closeSession(anyLong(), Mockito.any(), Mockito.any())).thenReturn(session);
        when(posApiClient.getZReport(anyLong())).thenReturn(z);

        assertDoesNotThrow(() -> {
            PosBlindCloseDialog dialog = new PosBlindCloseDialog(null, posApiClient, session, null);
            assertNotNull(dialog);
        });
    }

    @Test
    void testPosSessionHistoryDialogInstantiation() {
        PosSessionSummaryDTO s1 = new PosSessionSummaryDTO(
                1L, "operador", LocalDateTime.now().minusHours(4), LocalDateTime.now(),
                "CLOSED", BigDecimal.valueOf(100), BigDecimal.valueOf(500), BigDecimal.valueOf(500),
                BigDecimal.ZERO, BigDecimal.valueOf(400), 4
        );
        when(posApiClient.getSessionsHistory(anyLong())).thenReturn(List.of(s1));

        assertDoesNotThrow(() -> {
            PosSessionHistoryDialog dialog = new PosSessionHistoryDialog(null, posApiClient, 1L);
            assertNotNull(dialog);
        });
    }
}

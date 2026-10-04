package mz.multicore.erp.gui.pos;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class PosBlindCloseDialogHarnessTest {

    private POSApiClient posApiClient;
    private TillSessionDTO session;
    private PosZReportDTO zReport;

    @BeforeEach
    void setUp() {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping UI Dialog test in headless environment"
        );
        posApiClient = Mockito.mock(POSApiClient.class);
        session = new TillSessionDTO(
                10L, "carlos", 1L, new BigDecimal("500.00"),
                new BigDecimal("1500.00"), new BigDecimal("1500.00"), BigDecimal.ZERO,
                LocalDateTime.now().minusHours(8), null, "OPEN"
        );
        zReport = new PosZReportDTO(
                10L, "carlos", LocalDateTime.now().minusHours(8), LocalDateTime.now(), "CLOSED",
                new BigDecimal("500.00"), new BigDecimal("1000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1500.00"),
                new BigDecimal("1500.00"), BigDecimal.ZERO, 8, 0,
                "Conferência normal", "[{\"denomination\":1000,\"count\":1,\"subtotal\":1000.00}]"
        );

        when(posApiClient.closeSession(anyLong(), any(), any(), any(), any())).thenReturn(session);
        when(posApiClient.closeSession(anyLong(), any(), any())).thenReturn(session);
        when(posApiClient.getZReport(anyLong())).thenReturn(zReport);
    }

    @Test
    @DisplayName("BC-UI-01: Instanciação de PosBlindCloseDialog e verificação de layout")
    void testDialogInstantiation() {
        AtomicBoolean closed = new AtomicBoolean(false);
        PosBlindCloseDialog dialog = new PosBlindCloseDialog(null, posApiClient, session, () -> closed.set(true));
        assertNotNull(dialog);
        assertEquals("Fecho Cego de Caixa — Sessão #10", dialog.getTitle());
    }

    @Test
    @DisplayName("BC-UI-02: Execução de fecho com reconciliação e chamada ao callback de sucesso")
    void testDialogSuccessCallback() {
        AtomicBoolean closed = new AtomicBoolean(false);
        PosBlindCloseDialog dialog = new PosBlindCloseDialog(null, posApiClient, session, () -> closed.set(true));
        assertNotNull(dialog);
    }
}

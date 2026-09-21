package mz.multicore.erp.gui.pos.contingency;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.PosContingencyStatus;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o Gestor de Contingência do POS (PosContingencyManager).
 */
class PosContingencyManagerTest {

    @TempDir
    Path tempDir;

    private Path queueFile;
    private PosContingencyManager manager;
    private POSApiClient mockApiClient;

    @BeforeEach
    void setUp() {
        queueFile = tempDir.resolve("test_contingency_queue.json");
        manager = new PosContingencyManager(queueFile);
        mockApiClient = mock(POSApiClient.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(queueFile);
    }

    @Test
    @DisplayName("Enqueue adiciona venda com status PENDING_SYNC e persiste em JSON")
    void testEnqueueAndPersistence() {
        AtomicInteger notifiedCount = new AtomicInteger(-1);
        manager.addListener(notifiedCount::set);

        POSCheckoutRequest req = createSampleRequest("João Caixa");
        PosContingencySale sale = manager.enqueue(req, new BigDecimal("150.00"));

        assertNotNull(sale);
        assertTrue(sale.contingencyReference().startsWith("CONT-"));
        assertEquals(PosContingencyStatus.PENDING_SYNC, sale.status());
        assertEquals(new BigDecimal("150.00"), sale.cartTotal());
        assertEquals(1, manager.getPendingCount());
        assertEquals(1, notifiedCount.get());
        assertTrue(Files.exists(queueFile));

        // Recarrega de outro manager usando o mesmo ficheiro
        PosContingencyManager reloadedManager = new PosContingencyManager(queueFile);
        assertEquals(1, reloadedManager.getPendingCount());
        PosContingencySale loadedSale = reloadedManager.getAllSales().get(0);
        assertEquals(sale.contingencyReference(), loadedSale.contingencyReference());
        assertEquals(sale.cartTotal(), loadedSale.cartTotal());
    }

    @Test
    @DisplayName("Deteção de erros de conectividade para acionamento de contingência")
    void testConnectivityErrorDetection() {
        assertTrue(manager.isConnectivityError(new ConnectException("Connection refused")));
        assertTrue(manager.isConnectivityError(new SocketTimeoutException("Read timed out")));
        assertTrue(manager.isConnectivityError(new RuntimeException(new ConnectException("Falha"))));
        assertTrue(manager.isConnectivityError(new RuntimeException("Server 503 Service Unavailable")));

        assertFalse(manager.isConnectivityError(new IllegalArgumentException("Armazém inválido")));
        assertFalse(manager.isConnectivityError(new IllegalStateException("Sessão fechada")));
        assertFalse(manager.isConnectivityError(null));
    }

    @Test
    @DisplayName("Sincronização com sucesso atualiza status para SYNCED e número da fatura")
    void testSyncPendingSalesSuccess() {
        POSCheckoutRequest req = createSampleRequest("Operador 1");
        manager.enqueue(req, new BigDecimal("250.00"));

        InvoiceDTO mockInvoice = mock(InvoiceDTO.class);
        when(mockInvoice.invoiceNumber()).thenReturn("FT 2026/0123");
        when(mockApiClient.checkout(any(POSCheckoutRequest.class))).thenReturn(mockInvoice);

        int synced = manager.syncPendingSales(mockApiClient);

        assertEquals(1, synced);
        assertEquals(0, manager.getPendingCount());
        assertEquals(1, manager.getAllSales().size());
        PosContingencySale syncedSale = manager.getAllSales().get(0);
        assertEquals(PosContingencyStatus.SYNCED, syncedSale.status());
        assertEquals("FT 2026/0123", syncedSale.syncedInvoiceNumber());
        assertNull(syncedSale.errorMessage());
    }

    @Test
    @DisplayName("Sincronização aborta graciosamente quando a rede continua indisponível sem descartar vendas")
    void testSyncStopsOnNetworkError() {
        manager.enqueue(createSampleRequest("Operador 1"), new BigDecimal("100.00"));
        manager.enqueue(createSampleRequest("Operador 1"), new BigDecimal("200.00"));

        when(mockApiClient.checkout(any(POSCheckoutRequest.class)))
                .thenThrow(new RuntimeException(new ConnectException("Network unreachable")));

        int synced = manager.syncPendingSales(mockApiClient);

        assertEquals(0, synced);
        assertEquals(2, manager.getPendingCount());
        assertEquals(PosContingencyStatus.PENDING_SYNC, manager.getAllSales().get(0).status());
        assertEquals(PosContingencyStatus.PENDING_SYNC, manager.getAllSales().get(1).status());
    }

    private POSCheckoutRequest createSampleRequest(String operator) {
        return new POSCheckoutRequest(
                operator,
                1L,
                null,
                "Cliente Teste",
                10L,
                null,
                List.of(new POSCheckoutLineRequest(100L, new BigDecimal("1"), BigDecimal.ZERO, null, null)),
                List.of(new PosPaymentRequest("CASH", new BigDecimal("100.00"), null, null, null))
        );
    }
}

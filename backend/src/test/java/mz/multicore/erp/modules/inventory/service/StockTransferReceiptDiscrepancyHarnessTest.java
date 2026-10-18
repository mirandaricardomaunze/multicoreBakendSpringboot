package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest.ReceiveLine;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.StockTransferLineBatch;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Harness de {@code docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md}: na recepção, o recebido entra
 * no destino e a diferença volta à origem com o mesmo lote e validade; nada se perde.
 */
class StockTransferReceiptDiscrepancyHarnessTest {

    private static final long LINE_ID = 7L;

    private StockTransferRepository transferRepository;
    private StockRepository stockRepository;
    private StockMovementRepository stockMovementRepository;
    private ProductBatchService productBatchService;
    private AuditLogService audit;
    private StockTransferTransitService transitService;
    private StockTransferService service;

    private Company company;
    private Warehouse origin;
    private Warehouse destination;
    private Product product;

    @BeforeEach
    void setUp() {
        transferRepository = mock(StockTransferRepository.class);
        stockRepository = mock(StockRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        productBatchService = mock(ProductBatchService.class);
        audit = mock(AuditLogService.class);

        transitService = new StockTransferTransitService(
                transferRepository, stockRepository, stockMovementRepository, productBatchService, audit);
        service = new StockTransferService(
                transferRepository, mock(WarehouseRepository.class), mock(CompanyRepository.class),
                mock(ProductRepository.class), stockRepository, stockMovementRepository, productBatchService,
                mock(DocumentNumberService.class), audit,
                mock(org.springframework.context.ApplicationEventPublisher.class), null, transitService);

        company = new Company();
        company.setId(1L);
        origin = warehouse(10L, "Depósito");
        destination = warehouse(20L, "Loja");
        product = new Product();
        product.setId(100L);
        product.setSku("SKU-1");
        product.setName("Arroz");

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("joao", "ADMIN");
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndWarehouseId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(productBatchService.addToBatch(any(), any(), anyString(), any(), any()))
                .thenAnswer(inv -> batch(inv.getArgument(2), inv.getArgument(1)));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void recepcaoParcial_recebidoNoDestino_diferencaVoltaAOrigem() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        StockTransferDTO dto = transitService.receive(1L, "Faltaram 2 sacos",
                List.of(new ReceiveLine(LINE_ID, new BigDecimal("3"), "Falta na descarga")));

        assertEquals("RECEIVED_WITH_DISCREPANCY", dto.status());
        verify(productBatchService).addToBatch(eq(product), eq(destination), eq("LOTE-A"), any(), eq(new BigDecimal("3")));
        verify(productBatchService).addToBatch(eq(product), eq(origin), eq("LOTE-A"), any(), eq(new BigDecimal("2")));
        StockTransferLine line = t.getLines().get(0);
        assertEquals(0, new BigDecimal("3").compareTo(line.getReceivedQuantity()));
        assertEquals(0, new BigDecimal("2").compareTo(line.getReturnedQuantity()));
        assertEquals("Falta na descarga", line.getDiscrepancyReason());
        assertEquals(0, new BigDecimal("3").compareTo(dto.lines().get(0).receivedQuantity()));
        assertTrue(dto.lines().get(0).hasDiscrepancy());
        verify(audit).logCurrent(eq("STOCK_TRANSFER_RECEIVE_DISCREPANCY"), anyString());
    }

    @Test
    void invariante_enviadoIgualRecebidoMaisDevolvido() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        transitService.receive(1L, null, List.of(new ReceiveLine(LINE_ID, new BigDecimal("3"), "Avaria")));

        ArgumentCaptor<StockMovement> mv = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(mv.capture());
        BigDecimal soma = mv.getAllValues().stream().map(StockMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("5").compareTo(soma), "Tudo o que saiu tem de reentrar algures.");
        assertTrue(mv.getAllValues().stream().anyMatch(m -> m.getWarehouse() == origin
                && m.getDescription().contains("devolução à origem")));
    }

    @Test
    void variosLotes_recebidoConsomeParcelasPelaOrdemDoDespacho() {
        StockTransfer t = inTransit(parcel("LOTE-A", 3), parcel("LOTE-B", 2));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        transitService.receive(1L, null, List.of(new ReceiveLine(LINE_ID, new BigDecimal("4"), "Extravio")));

        verify(productBatchService).addToBatch(eq(product), eq(destination), eq("LOTE-A"), any(), eq(new BigDecimal("3")));
        verify(productBatchService).addToBatch(eq(product), eq(destination), eq("LOTE-B"), any(), eq(new BigDecimal("1")));
        verify(productBatchService).addToBatch(eq(product), eq(origin), eq("LOTE-B"), any(), eq(new BigDecimal("1")));
        verify(productBatchService, never()).addToBatch(eq(product), eq(origin), eq("LOTE-A"), any(), any());
    }

    @Test
    void recebidoZero_tudoVoltaAOrigem() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        StockTransferDTO dto = transitService.receive(1L, null,
                List.of(new ReceiveLine(LINE_ID, BigDecimal.ZERO, "Mercadoria não chegou")));

        assertEquals("RECEIVED_WITH_DISCREPANCY", dto.status());
        verify(productBatchService, never()).addToBatch(any(), eq(destination), any(), any(), any());
        verify(productBatchService).addToBatch(eq(product), eq(origin), eq("LOTE-A"), any(), eq(new BigDecimal("5")));
    }

    @Test
    void divergenciaSemMotivo_eRecusadaSemMexerNoStock() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> transitService.receive(1L, null,
                List.of(new ReceiveLine(LINE_ID, new BigDecimal("3"), "  "))));

        assertTrue(ex.getMessage().contains("motivo da divergência"));
        assertEquals(TransferStatus.IN_TRANSIT, t.getStatus());
        verify(productBatchService, never()).addToBatch(any(), any(), any(), any(), any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void recebidoAcimaDoEnviado_eRecusado() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> transitService.receive(1L, null,
                List.of(new ReceiveLine(LINE_ID, new BigDecimal("6"), null))));

        assertTrue(ex.getMessage().contains("excede a enviada"));
        assertEquals(TransferStatus.IN_TRANSIT, t.getStatus());
    }

    @Test
    void linhaDeOutraGuia_eRecusada() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> transitService.receive(1L, null,
                List.of(new ReceiveLine(999L, new BigDecimal("5"), null))));
        assertEquals(TransferStatus.IN_TRANSIT, t.getStatus());
    }

    @Test
    void recepcaoIntegralExplicita_ficaRecebidaSemDivergencia() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        StockTransferDTO dto = transitService.receive(1L, null,
                List.of(new ReceiveLine(LINE_ID, new BigDecimal("5"), null)));

        assertEquals("RECEIVED", dto.status());
        verify(productBatchService, never()).addToBatch(any(), eq(origin), any(), any(), any());
        assertEquals(0, BigDecimal.ZERO.compareTo(t.getLines().get(0).getReturnedQuantity()));
        verify(audit).logCurrent(eq("STOCK_TRANSFER_RECEIVE"), anyString());
    }

    @Test
    void pedidoAntigoSemLinhas_mantemRecepcaoIntegral() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        StockTransferDTO dto = transitService.receive(1L, new ReceiveStockTransferRequest("ok").notes());

        assertEquals("RECEIVED", dto.status());
        assertTrue(new ReceiveStockTransferRequest("ok").lines().isEmpty());
    }

    @Test
    void recebidaComDivergencia_eTerminal_imprimeMasNaoCancelaNemRecebeDeNovo() {
        StockTransfer t = inTransit(parcel("LOTE-A", 5));
        t.setStatus(TransferStatus.RECEIVED_WITH_DISCREPANCY);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        assertSame(t, service.loadForPrint(1L));
        assertThrows(BusinessRuleException.class, () -> service.cancel(1L));
        assertThrows(BusinessRuleException.class, () -> transitService.receive(1L, null));
        assertEquals(TransferStatus.RECEIVED_WITH_DISCREPANCY, t.getStatus());
    }

    // ───────────────────────── helpers ─────────────────────────

    private StockTransferLineBatch parcel(String number, int qty) {
        StockTransferLineBatch p = new StockTransferLineBatch();
        p.setBatchNumber(number);
        p.setExpirationDate(LocalDate.now().plusDays(30));
        p.setQuantity(new BigDecimal(qty));
        return p;
    }

    private StockTransfer inTransit(StockTransferLineBatch... parcels) {
        StockTransfer t = new StockTransfer();
        t.setId(1L);
        t.setTransferNumber("TRF-2026/1");
        t.setTransferDate(LocalDateTime.now());
        t.setCompany(company);
        t.setOriginWarehouse(origin);
        t.setDestinationWarehouse(destination);
        t.setStatus(TransferStatus.IN_TRANSIT);
        t.setRequiresReceipt(true);
        t.setCreatedBy("joao");
        StockTransferLine line = new StockTransferLine();
        line.setId(LINE_ID);
        line.setTransfer(t);
        line.setProduct(product);
        BigDecimal total = BigDecimal.ZERO;
        for (StockTransferLineBatch p : parcels) {
            p.setLine(line);
            line.getTransitBatches().add(p);
            total = total.add(p.getQuantity());
        }
        line.setQuantity(total);
        t.getLines().add(line);
        return t;
    }

    private Warehouse warehouse(long id, String name) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setName(name);
        w.setCompany(company);
        return w;
    }

    private ProductBatch batch(String number, Warehouse warehouse) {
        ProductBatch b = new ProductBatch();
        b.setProduct(product);
        b.setWarehouse(warehouse);
        b.setBatchNumber(number);
        b.setExpirationDate(LocalDate.now().plusDays(30));
        b.setQuantity(new BigDecimal("5"));
        return b;
    }
}

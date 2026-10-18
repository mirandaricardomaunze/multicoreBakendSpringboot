package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Harness de {@code docs/STOCK_IN_TRANSIT_SPEC.md}: o despacho só tira da origem, a recepção só põe
 * no destino, e {@code requiresReceipt=false} mantém o fluxo de 1 passo.
 */
class StockTransferInTransitHarnessTest {

    private StockTransferRepository transferRepository;
    private StockRepository stockRepository;
    private StockMovementRepository stockMovementRepository;
    private ProductBatchService productBatchService;
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
        AuditLogService audit = mock(AuditLogService.class);

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
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void despacho_soTiraDaOrigem_eFicaEmTransito() {
        StockTransfer t = transfer(TransferStatus.SEPARATED, true);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));
        stubFefo();

        StockTransferDTO dto = service.approve(1L);

        assertEquals("IN_TRANSIT", dto.status());
        assertTrue(dto.requiresReceipt());
        assertNotNull(dto.dispatchedAt());
        verify(productBatchService).consumeFEFO(product, origin, new BigDecimal("5"));
        // O destino não é tocado: nem lote, nem stock.
        verify(productBatchService, never()).addToBatch(any(), eq(destination), any(), any(), any());
        ArgumentCaptor<StockMovement> mv = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(1)).save(mv.capture());
        assertEquals(new BigDecimal("-5"), mv.getValue().getQuantity());
        assertEquals(1, t.getLines().get(0).getTransitBatches().size());
    }

    @Test
    void recepcao_soPoeNoDestino_eFicaRecebida() {
        StockTransfer t = inTransit();
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));
        when(productBatchService.addToBatch(eq(product), eq(destination), eq("LOTE-A"), any(), eq(new BigDecimal("5"))))
                .thenReturn(batch("LOTE-A"));

        StockTransferDTO dto = transitService.receive(1L, "Chegou tudo");

        assertEquals("RECEIVED", dto.status());
        assertEquals("joao", dto.receivedBy());
        assertEquals("Chegou tudo", dto.receiptNotes());
        verify(productBatchService).addToBatch(eq(product), eq(destination), eq("LOTE-A"), any(), eq(new BigDecimal("5")));
        verify(productBatchService, never()).consumeFEFO(any(), any(), any());
        ArgumentCaptor<Stock> st = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(st.capture());
        assertEquals(new BigDecimal("5"), st.getValue().getQuantity());
        assertEquals(destination, st.getValue().getWarehouse());
    }

    @Test
    void invariante_saidaOrigemIgualEntradaDestino() {
        StockTransfer t = transfer(TransferStatus.SEPARATED, true);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));
        stubFefo();
        service.approve(1L);
        when(productBatchService.addToBatch(any(), eq(destination), any(), any(), any())).thenReturn(batch("LOTE-A"));

        transitService.receive(1L, null);

        ArgumentCaptor<StockMovement> mv = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(mv.capture());
        BigDecimal soma = mv.getAllValues().stream().map(StockMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, soma.compareTo(BigDecimal.ZERO), "O que sai da origem tem de entrar no destino.");
    }

    @Test
    void duplaRecepcao_eRecusada() {
        StockTransfer t = inTransit();
        t.setStatus(TransferStatus.RECEIVED);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> transitService.receive(1L, null));
        assertTrue(ex.getMessage().contains("em trânsito"));
        verify(productBatchService, never()).addToBatch(any(), any(), any(), any(), any());
    }

    @Test
    void recepcao_semPerfilGestor_eRecusada() {
        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        StockTransfer t = inTransit();
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> transitService.receive(1L, null));
        assertEquals(TransferStatus.IN_TRANSIT, t.getStatus());
    }

    @Test
    void recepcao_antesDoDespacho_eRecusada() {
        StockTransfer t = transfer(TransferStatus.SEPARATED, true);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> transitService.receive(1L, null));
    }

    @Test
    void emTransito_naoPodeSerCanceladaNemRejeitada() {
        StockTransfer t = inTransit();
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> service.cancel(1L));
        assertThrows(BusinessRuleException.class, () -> service.reject(1L, "motivo"));
        assertEquals(TransferStatus.IN_TRANSIT, t.getStatus());
    }

    @Test
    void semRequiresReceipt_mantemFluxoDeUmPasso() {
        StockTransfer t = transfer(TransferStatus.SEPARATED, false);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));
        stubFefo();

        StockTransferDTO dto = service.approve(1L);

        assertEquals("APPROVED", dto.status());
        assertFalse(dto.requiresReceipt());
        verify(productBatchService).addToBatch(eq(product), eq(destination), eq("LOTE-A"), any(), eq(new BigDecimal("5")));
        verify(stockMovementRepository, times(2)).save(any());
    }

    @Test
    void guiaEmTransito_podeImprimirGuiaOficial() {
        StockTransfer t = inTransit();
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(t));

        assertSame(t, service.loadForPrint(1L));
    }

    // ───────────────────────── helpers ─────────────────────────

    private void stubFefo() {
        when(productBatchService.consumeFEFO(eq(product), eq(origin), eq(new BigDecimal("5"))))
                .thenReturn(List.of(new ProductBatchService.BatchConsumption(
                        batch("LOTE-A"), new BigDecimal("5"))));
    }

    private StockTransfer inTransit() {
        StockTransfer t = transfer(TransferStatus.IN_TRANSIT, true);
        var parcel = new mz.multicore.erp.modules.inventory.model.StockTransferLineBatch();
        parcel.setLine(t.getLines().get(0));
        parcel.setBatchNumber("LOTE-A");
        parcel.setExpirationDate(LocalDate.now().plusDays(30));
        parcel.setQuantity(new BigDecimal("5"));
        t.getLines().get(0).getTransitBatches().add(parcel);
        return t;
    }

    private StockTransfer transfer(TransferStatus status, boolean requiresReceipt) {
        StockTransfer t = new StockTransfer();
        t.setId(1L);
        t.setTransferNumber("TRF-2026/1");
        t.setTransferDate(LocalDateTime.now());
        t.setCompany(company);
        t.setOriginWarehouse(origin);
        t.setDestinationWarehouse(destination);
        t.setStatus(status);
        t.setRequiresReceipt(requiresReceipt);
        t.setCreatedBy("joao");
        StockTransferLine line = new StockTransferLine();
        line.setTransfer(t);
        line.setProduct(product);
        line.setQuantity(new BigDecimal("5"));
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

    private ProductBatch batch(String number) {
        ProductBatch b = new ProductBatch();
        b.setProduct(product);
        b.setWarehouse(origin);
        b.setBatchNumber(number);
        b.setExpirationDate(LocalDate.now().plusDays(30));
        b.setQuantity(new BigDecimal("5"));
        return b;
    }
}

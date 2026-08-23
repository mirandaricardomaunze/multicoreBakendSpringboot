package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
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
 * Testes da máquina de estados da guia de transferência. Foco: o stock só sai na APROVAÇÃO,
 * só MANAGER/ADMIN aprovam/rejeitam, e rejeitar/cancelar nunca movem stock. Repositórios e
 * FEFO mockados — não levanta o Spring.
 */
class StockTransferServiceTest {

    private StockTransferRepository transferRepository;
    private WarehouseRepository warehouseRepository;
    private CompanyRepository companyRepository;
    private ProductRepository productRepository;
    private StockRepository stockRepository;
    private StockMovementRepository stockMovementRepository;
    private ProductBatchService productBatchService;
    private DocumentNumberService documentNumberService;
    private AuditLogService auditLogService;
    private StockTransferService service;

    private Company company;
    private Warehouse origin;
    private Warehouse destination;
    private Product product;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        transferRepository = mock(StockTransferRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        companyRepository = mock(CompanyRepository.class);
        productRepository = mock(ProductRepository.class);
        stockRepository = mock(StockRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        productBatchService = mock(ProductBatchService.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditLogService = mock(AuditLogService.class);

        // A transferência anuncia o desfecho por evento, para o inventário não passar a conhecer o
        // comercial (ver StockTransferResolvedEvent). Aqui basta um publicador mudo.
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);

        service = new StockTransferService(
                transferRepository, warehouseRepository, companyRepository, productRepository,
                stockRepository, stockMovementRepository, productBatchService, documentNumberService,
                auditLogService, eventPublisher);

        company = company(1L);
        origin = warehouse(10L, "Depósito", company);
        destination = warehouse(20L, "Loja", company);
        product = product(100L, "SKU-1", "Arroz");

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("joao", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ────────────────────────── create ──────────────────────────

    @Test
    void create_ficaPendente_eNaoMoveStock() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(origin));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(destination));
        when(productRepository.findByIdAndCompaniesId(100L, 1L)).thenReturn(Optional.of(product));
        when(documentNumberService.next(any())).thenReturn("TRF-2026/1");
        when(productBatchService.sumQuantity(100L, 10L)).thenReturn(new BigDecimal("50"));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        StockTransferDTO dto = service.create(request(new BigDecimal("5")));

        assertEquals(TransferStatus.PENDING_APPROVAL.name(), dto.status());
        // O stock NÃO se move na criação.
        verify(productBatchService, never()).consumeFEFO(any(), any(), any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void create_semStockSuficiente_lancaExcecao() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(origin));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(destination));
        when(productRepository.findByIdAndCompaniesId(100L, 1L)).thenReturn(Optional.of(product));
        when(documentNumberService.next(any())).thenReturn("TRF-2026/1");
        when(productBatchService.sumQuantity(100L, 10L)).thenReturn(new BigDecimal("2"));

        assertThrows(BusinessRuleException.class, () -> service.create(request(new BigDecimal("5"))));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void create_stockAntigoSemLotes_eAceite() { // RI: disponibilidade = a mesma da saída autoritativa
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(origin));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(destination));
        when(productRepository.findByIdAndCompaniesId(100L, 1L)).thenReturn(Optional.of(product));
        when(documentNumberService.next(any())).thenReturn("TRF-2026/1");
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        // Instalação anterior aos lotes (e a base de demonstração): stocks preenchido, lotes vazios.
        // Os lotes são materializados preguiçosamente na primeira saída, por isso perguntar só a
        // eles dava zero em stock que existe — e a guia era recusada por falta do que lá estava.
        when(productBatchService.sumQuantity(100L, 10L)).thenReturn(BigDecimal.ZERO);
        Stock legacy = new Stock();
        legacy.setQuantity(new BigDecimal("85"));
        when(stockRepository.findByProductIdAndWarehouseId(100L, 10L)).thenReturn(Optional.of(legacy));

        StockTransferDTO dto = service.create(request(new BigDecimal("5")));

        assertEquals(TransferStatus.PENDING_APPROVAL.name(), dto.status());
        verify(productBatchService, never()).consumeFEFO(any(), any(), any());
    }

    @Test
    void create_semStockNemLotes_continuaARecusar() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(origin));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(destination));
        when(productRepository.findByIdAndCompaniesId(100L, 1L)).thenReturn(Optional.of(product));
        when(documentNumberService.next(any())).thenReturn("TRF-2026/1");
        when(productBatchService.sumQuantity(100L, 10L)).thenReturn(BigDecimal.ZERO);
        when(stockRepository.findByProductIdAndWarehouseId(100L, 10L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.create(request(new BigDecimal("5"))));
        verify(transferRepository, never()).save(any());
    }

    // ────────────────────────── approve ──────────────────────────

    @Test
    void approve_comAdmin_moveStock_eFicaAprovada() {
        StockTransfer pending = pendingTransfer(new BigDecimal("5"));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(pending));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductBatch batch = batch("LOTE-A", LocalDate.now().plusDays(30));
        when(productBatchService.consumeFEFO(eq(product), eq(origin), eq(new BigDecimal("5"))))
                .thenReturn(List.of(new ProductBatchService.BatchConsumption(batch, new BigDecimal("5"))));
        when(stockRepository.findByProductIdAndWarehouseId(anyLong(), anyLong())).thenReturn(Optional.empty());

        StockTransferDTO dto = service.approve(1L);

        assertEquals(TransferStatus.APPROVED.name(), dto.status());
        assertEquals("joao", dto.approvedBy());
        verify(productBatchService).consumeFEFO(product, origin, new BigDecimal("5"));
        verify(productBatchService).addToBatch(eq(product), eq(destination), eq("LOTE-A"), any(), eq(new BigDecimal("5")));
        // Dois movimentos TRANSFER por lote: saída na origem + entrada no destino.
        verify(stockMovementRepository, times(2)).save(any());
    }

    @Test
    void approve_semPerfilAutorizado_lancaExcecao_eNaoMoveStock() {
        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        StockTransfer pending = pendingTransfer(new BigDecimal("5"));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(pending));

        assertThrows(BusinessRuleException.class, () -> service.approve(1L));
        verify(productBatchService, never()).consumeFEFO(any(), any(), any());
        verify(stockMovementRepository, never()).save(any());
        assertEquals(TransferStatus.PENDING_APPROVAL, pending.getStatus());
    }

    @Test
    void approve_guiaJaAprovada_lancaExcecao() {
        StockTransfer approved = pendingTransfer(new BigDecimal("5"));
        approved.setStatus(TransferStatus.APPROVED);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(approved));

        assertThrows(BusinessRuleException.class, () -> service.approve(1L));
        verify(productBatchService, never()).consumeFEFO(any(), any(), any());
    }

    // ────────────────────────── reject / cancel ──────────────────────────

    @Test
    void reject_naoMoveStock_eGuardaMotivo() {
        StockTransfer pending = pendingTransfer(new BigDecimal("5"));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(pending));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        StockTransferDTO dto = service.reject(1L, "Pedido duplicado");

        assertEquals(TransferStatus.REJECTED.name(), dto.status());
        assertEquals("Pedido duplicado", dto.rejectionReason());
        verify(productBatchService, never()).consumeFEFO(any(), any(), any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void reject_semMotivo_lancaExcecao() {
        StockTransfer pending = pendingTransfer(new BigDecimal("5"));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(pending));

        assertThrows(BusinessRuleException.class, () -> service.reject(1L, "  "));
    }

    @Test
    void cancel_pendente_ficaCancelada() {
        StockTransfer pending = pendingTransfer(new BigDecimal("5"));
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(pending));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        StockTransferDTO dto = service.cancel(1L);

        assertEquals(TransferStatus.CANCELLED.name(), dto.status());
    }

    @Test
    void cancel_aprovada_lancaExcecao() {
        StockTransfer approved = pendingTransfer(new BigDecimal("5"));
        approved.setStatus(TransferStatus.APPROVED);
        when(transferRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(approved));

        assertThrows(BusinessRuleException.class, () -> service.cancel(1L));
    }

    // ────────────────────────── helpers ──────────────────────────

    private CreateStockTransferRequest request(BigDecimal qty) {
        return new CreateStockTransferRequest(
                1L, 10L, 20L, "João", null, null,
                List.of(new CreateStockTransferLineRequest(100L, qty)));
    }

    private StockTransfer pendingTransfer(BigDecimal qty) {
        StockTransfer t = new StockTransfer();
        t.setId(1L);
        t.setTransferNumber("TRF-2026/1");
        t.setTransferDate(LocalDateTime.now());
        t.setCompany(company);
        t.setOriginWarehouse(origin);
        t.setDestinationWarehouse(destination);
        t.setStatus(TransferStatus.PENDING_APPROVAL);
        t.setCreatedBy("joao");
        StockTransferLine line = new StockTransferLine();
        line.setTransfer(t);
        line.setProduct(product);
        line.setQuantity(qty);
        t.getLines().add(line);
        return t;
    }

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Warehouse warehouse(long id, String name, Company company) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setName(name);
        w.setCompany(company);
        return w;
    }

    private static Product product(long id, String sku, String name) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        return p;
    }

    private ProductBatch batch(String number, LocalDate exp) {
        ProductBatch b = new ProductBatch();
        b.setProduct(product);
        b.setWarehouse(origin);
        b.setBatchNumber(number);
        b.setExpirationDate(exp);
        b.setQuantity(new BigDecimal("5"));
        return b;
    }
}

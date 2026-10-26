package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.CreateStockAdjustmentRequest;
import mz.multicore.erp.modules.inventory.model.StockAdjustmentOperation;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.model.StockMovementOperation;
import mz.multicore.erp.modules.inventory.dto.RegisterMovementRequest;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockMovementOperationRepository;
import mz.multicore.erp.modules.inventory.repository.StockAdjustmentOperationRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes do alerta de validade ({@link InventoryService#findExpiringBatches}). Garante que o
 * horizonte é convertido para data de corte (hoje + dias), que a empresa activa é exigida e que
 * a consulta é delegada no {@link ProductBatchService}. Dependências mockadas.
 */
class InventoryServiceTest {

    private static final Long COMPANY_ID = 1L;

    private ProductBatchService productBatchService;
    private WarehouseRepository warehouseRepository;
    private StockRepository stockRepository;
    private ProductRepository productRepository;
    private StockMovementRepository stockMovementRepository;
    private StockMovementOperationRepository stockMovementOperationRepository;
    private StockAdjustmentOperationRepository adjustmentOperationRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        productBatchService = mock(ProductBatchService.class);
        warehouseRepository = mock(WarehouseRepository.class);
        stockRepository = mock(StockRepository.class);
        productRepository = mock(ProductRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        stockMovementOperationRepository = mock(StockMovementOperationRepository.class);
        adjustmentOperationRepository = mock(StockAdjustmentOperationRepository.class);
        service = new InventoryService(
                warehouseRepository,
                stockRepository,
                stockMovementRepository,
                stockMovementOperationRepository,
                adjustmentOperationRepository,
                mock(CompanyRepository.class),
                productBatchService,
                productRepository,
                mock(AuditLogService.class));

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void findExpiringBatches_usaCorteHojeMaisDias_eDelega() {
        ProductBatchDTO dto = batch(LocalDate.now().plusDays(5));
        when(productBatchService.findExpiringByCompany(eq(COMPANY_ID), any(LocalDate.class)))
                .thenReturn(List.of(dto));

        List<ProductBatchDTO> result = service.findExpiringBatches(COMPANY_ID, 30);

        assertEquals(1, result.size());
        verify(productBatchService).findExpiringByCompany(COMPANY_ID, LocalDate.now().plusDays(30));
    }

    @Test
    void findExpiringBatches_empresaDiferenteDaActiva_lancaBusinessRule() {
        assertThrows(BusinessRuleException.class,
                () -> service.findExpiringBatches(999L, 30));
        verifyNoInteractions(productBatchService);
    }

    @Test
    void getSalesWarehouses_soActivosEQuePermitemVendas() {
        mz.multicore.erp.modules.inventory.model.Warehouse loja = warehouse("Loja", true, true);
        mz.multicore.erp.modules.inventory.model.Warehouse deposito = warehouse("Depósito", true, false);   // não vende
        mz.multicore.erp.modules.inventory.model.Warehouse inactivo = warehouse("Antigo", false, true);     // inactivo
        when(warehouseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(loja, deposito, inactivo));

        List<mz.multicore.erp.modules.inventory.model.Warehouse> vendas = service.getSalesWarehousesByCompany(COMPANY_ID);
        assertEquals(1, vendas.size());
        assertEquals("Loja", vendas.get(0).getName());

        // getWarehousesByCompany exclui inactivos, mas mantém depósitos.
        assertEquals(2, service.getWarehousesByCompany(COMPANY_ID).size());
    }

    @Test
    void getInStockProductIdsForSale_soProdutosComQuantidadePositivaEmArmazemDeVenda() {
        mz.multicore.erp.modules.inventory.model.Warehouse loja = warehouse("Loja", true, true);
        loja.setId(10L);
        mz.multicore.erp.modules.inventory.model.Warehouse deposito = warehouse("Depósito", true, false); // não vende
        deposito.setId(20L);
        when(warehouseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(loja, deposito));
        when(stockRepository.findByWarehouseId(10L)).thenReturn(List.of(
                stock(1L, new BigDecimal("5")),    // disponível
                stock(2L, BigDecimal.ZERO),        // esgotado
                stock(3L, new BigDecimal("-1"))));  // negativo → esgotado
        // O depósito (não vende) não deve sequer ser consultado.

        java.util.Set<Long> ids = service.getInStockProductIdsForSale(COMPANY_ID);

        assertEquals(java.util.Set.of(1L), ids);
        verify(stockRepository, never()).findByWarehouseId(20L);
    }

    @Test
    void findOutOfStockProducts_soComControloDeStockESaldoTotalNaoPositivo() {
        // Stocks: produto 1 = 5 (ok); produto 4 espalhado por dois armazéns 2 e −3 = −1 (esgotado).
        when(stockRepository.findByWarehouseCompanyId(COMPANY_ID)).thenReturn(List.of(
                stock(1L, new BigDecimal("5")),
                stock(4L, new BigDecimal("2")),
                stock(4L, new BigDecimal("-3"))));
        // Catálogo: 1 (com stock), 2 (sem stock → esgotado), 3 (serviço, ignorado), 4 (saldo −1 → esgotado).
        when(productRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID)).thenReturn(List.of(
                product(1L, "SKU1", "Arroz", true),
                product(2L, "SKU2", "Feijão", true),
                product(3L, "SKU3", "Consulta", false),
                product(4L, "SKU4", "Óleo", true)));

        List<mz.multicore.erp.modules.inventory.dto.StockAlertDTO> out = service.findOutOfStockProducts(COMPANY_ID);

        java.util.Set<Long> ids = out.stream()
                .map(mz.multicore.erp.modules.inventory.dto.StockAlertDTO::productId)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(java.util.Set.of(2L, 4L), ids);
    }

    @Test
    void warehouseScopedReads_rejeitamArmazemDeOutraEmpresaAntesDaConsulta() {
        var foreignCompany = new mz.multicore.erp.modules.company.model.Company();
        foreignCompany.setId(99L);
        var foreignWarehouse = warehouse("Outra empresa", true, true);
        foreignWarehouse.setId(90L);
        foreignWarehouse.setCompany(foreignCompany);
        when(warehouseRepository.findById(90L)).thenReturn(java.util.Optional.of(foreignWarehouse));

        assertThrows(BusinessRuleException.class, () -> service.findStocksByWarehouse(90L));
        assertThrows(BusinessRuleException.class, () -> service.findBatchesByWarehouse(90L));
        assertThrows(BusinessRuleException.class, () -> service.findNextFEFO(100L, 90L));

        verify(stockRepository, never()).findByWarehouseId(90L);
        verify(productBatchService, never()).findByWarehouse(90L);
        verify(productBatchService, never()).findNextFEFO(anyLong(), eq(90L));
    }

    @Test
    void adjustStock_replayIdentico_devolveMovimentoSemNovoEfeito() {
        var product = product(100L, "SKU-100", "Arroz", true);
        var warehouse = warehouse("Central", true, true);
        warehouse.setId(10L);
        StockMovement movement = new StockMovement();
        movement.setId(77L);
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(new BigDecimal("2"));
        movement.setMovementType(StockMovementType.ADJUSTMENT);
        StockAdjustmentOperation operation = new StockAdjustmentOperation();
        operation.setRequestFingerprint(fingerprint(1L, 100L, 10L, "12", "Contagem física"));
        operation.setStockMovement(movement);
        when(adjustmentOperationRepository.findByCompanyIdAndIdempotencyKey(1L, "ADJUST-1"))
                .thenReturn(java.util.Optional.of(operation));

        var result = service.adjustStock(new CreateStockAdjustmentRequest(
                1L, 100L, 10L, new BigDecimal("12.0"), "Contagem física", "ADJUST-1"));

        assertEquals(77L, result.id());
        verifyNoInteractions(stockRepository, productBatchService);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void adjustStock_mesmaChaveComPayloadDiferente_rejeitaAntesDoStock() {
        StockAdjustmentOperation operation = new StockAdjustmentOperation();
        operation.setRequestFingerprint("outro-fingerprint");
        when(adjustmentOperationRepository.findByCompanyIdAndIdempotencyKey(1L, "ADJUST-2"))
                .thenReturn(java.util.Optional.of(operation));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.adjustStock(
                new CreateStockAdjustmentRequest(1L, 100L, 10L, new BigDecimal("8"), "Acerto", "ADJUST-2")));

        assertTrue(error.getMessage().contains("chave de idempotência"));
        verifyNoInteractions(stockRepository, productBatchService);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void registerMovement_replayIdentico_devolveMovimentoSemNovoEfeito() {
        var product = product(100L, "SKU-100", "Arroz", true);
        var warehouse = warehouse("Central", true, true);
        warehouse.setId(10L);
        StockMovement movement = new StockMovement();
        movement.setId(88L);
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(new BigDecimal("3"));
        movement.setMovementType(StockMovementType.ENTRY);
        StockMovementOperation operation = new StockMovementOperation();
        RegisterMovementRequest request = new RegisterMovementRequest(
                100L, 10L, new BigDecimal("3.0"), "ENTRY", "L-1", null,
                "Entrada", LocalDate.of(2027, 1, 1), "MOVEMENT-1");
        operation.setRequestFingerprint(movementFingerprint(request));
        operation.setStockMovement(movement);
        when(stockMovementOperationRepository.findByCompanyIdAndIdempotencyKey(1L, "MOVEMENT-1"))
                .thenReturn(java.util.Optional.of(operation));

        var result = service.registerMovement(request);

        assertEquals(88L, result.id());
        verifyNoInteractions(stockRepository, productBatchService);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void registerMovement_mesmaChaveComPayloadDiferente_rejeitaAntesDoStock() {
        StockMovementOperation operation = new StockMovementOperation();
        operation.setRequestFingerprint("outro-fingerprint");
        when(stockMovementOperationRepository.findByCompanyIdAndIdempotencyKey(1L, "MOVEMENT-2"))
                .thenReturn(java.util.Optional.of(operation));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.registerMovement(
                new RegisterMovementRequest(100L, 10L, BigDecimal.ONE, "ENTRY", null, null,
                        "Entrada", null, "MOVEMENT-2")));

        assertTrue(error.getMessage().contains("chave de idempotência"));
        verifyNoInteractions(stockRepository, productBatchService);
        verify(stockMovementRepository, never()).save(any());
    }

    private static String fingerprint(Long companyId, Long productId, Long warehouseId,
                                      String counted, String reason) {
        String canonical = companyId + "|" + productId + "|" + warehouseId + "|"
                + new BigDecimal(counted).stripTrailingZeros().toPlainString() + "|" + reason;
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String movementFingerprint(RegisterMovementRequest request) {
        String canonical = COMPANY_ID + "|" + request.productId() + "|" + request.warehouseId() + "|"
                + request.quantity().stripTrailingZeros().toPlainString() + "|" + request.movementType().trim()
                + "|" + value(request.batchNumber()) + "|" + value(request.serialNumber()) + "|"
                + value(request.description()) + "|" + value(request.expirationDate());
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String value(Object value) {
        return value == null ? "<null>" : value.toString().trim();
    }

    private mz.multicore.erp.modules.comercial.model.Product product(Long id, String sku, String name, boolean stockTracked) {
        mz.multicore.erp.modules.comercial.model.Product p = new mz.multicore.erp.modules.comercial.model.Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setStockTracked(stockTracked);
        return p;
    }

    private mz.multicore.erp.modules.inventory.model.Stock stock(Long productId, BigDecimal qty) {
        mz.multicore.erp.modules.comercial.model.Product p = new mz.multicore.erp.modules.comercial.model.Product();
        p.setId(productId);
        mz.multicore.erp.modules.inventory.model.Stock s = new mz.multicore.erp.modules.inventory.model.Stock();
        s.setProduct(p);
        s.setQuantity(qty);
        return s;
    }

    private mz.multicore.erp.modules.inventory.model.Warehouse warehouse(String name, boolean active, boolean allowsSales) {
        mz.multicore.erp.modules.inventory.model.Warehouse w = new mz.multicore.erp.modules.inventory.model.Warehouse();
        w.setName(name);
        w.setActive(active);
        w.setAllowsSales(allowsSales);
        return w;
    }

    private ProductBatchDTO batch(LocalDate expiration) {
        return new ProductBatchDTO(
                1L, 100L, "SKU-1", "Leite", 10L, "Loja Central",
                "EXP-" + expiration, expiration, LocalDate.now(), new BigDecimal("12"));
    }
}
